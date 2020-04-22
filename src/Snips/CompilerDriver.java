package Snips;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import CGen.LabelGen;
import CGen.Opt.ASMOptimizer;
import Ctx.ContextChecker;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.Structural.ASMComment;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Directive.Directive;
import Imm.AST.Directive.IncludeDirective;
import Imm.AST.Expression.Atom;
import Imm.AST.Statement.Declaration;
import Imm.AsN.AsNBody;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Parser;
import Par.Scanner;
import Par.Token;
import Par.Token.TokenType;
import PreP.PreProcessor;
import PreP.PreProcessor.LineObject;
import Util.Source;
import Util.Util;
import Util.XMLParser.XMLNode;
import Util.Logging.Message;

public class CompilerDriver {

	/* The Input File */
	public static File file;
	
	public static String [] logo = {
		"	 _______  __    _  ___  _______  _______",
		"	|       ||  \\  | ||   ||       ||       |",
		"	|  _____||   \\_| ||   ||    _  ||  _____|",
		"	| |_____ |       ||   ||   |_| || |_____",
		"	|_____  ||  _    ||   ||    ___||_____  |",
		"	 _____| || | \\   ||   ||   |     _____| |",
		"	|_______||_|  \\__||___||___|    |_______|"};
	
	public static List<Message> log = new ArrayList();
	
	/* Flags & Settings */
	public static boolean 
		logoPrinted = false, 
		useTerminalColors = true, 
		silenced = true,
		imm = false,
		enableComments = true,
		disableOptimizer = false,
		disableWarnings = false;
			
	/* Debug */
	public static boolean
		printProvisoTypes = false,
		includeProvisoInTypeString = false,
		printObjectIDs = false;
	
	public static String outputPath;
	
	public static boolean printErrors = false;
	
	public static String printDepth = "    ";
	public static int commentDistance = 30;
	
	public static List<Double> compressions = new ArrayList();
	
	public static int instructionsGenerated = 0;
	
	public static CompilerDriver driver;
	
	public static XMLNode sys_config;
	
	public static void main(String [] args) {
		/* Check if filepath argument was passed */
		if (args.length == 0) {
			System.out.println(new Message("No input file specified! See -help for argument information.", Message.Type.FAIL).getMessage());
			System.exit(0);
		}
		
		/* Instantiate new Compiler instance with filepath as argument */
		CompilerDriver scd = new CompilerDriver(args);
		driver = scd;
		
		if (outputPath == null) {
			outputPath = args [0];
			if (outputPath.contains("\\")) {
				while (!outputPath.endsWith("\\")) outputPath = outputPath.substring(0, outputPath.length() - 1);
			}
			
			outputPath += "out.s";
		}
		
		/* Errors occurred due to faulty parameters, abort */
		if (!log.isEmpty()) {
			log.add(new Message("Aborting.", Message.Type.FAIL));
			log.stream().forEach(x -> System.out.println(x.getMessage()));
			log.clear();
			System.exit(0);
		}
		
		List<String> code = Util.readFile(file);
		
		/* Perform compilation */
		scd.compile(file, code);
	}
	
	/* Reserved Declarations */
	public static Source nullSource = new Source("Default", 0, 0);
	public static Atom zero_atom = new Atom(new INT("0"), new Token(TokenType.INTLIT, nullSource), nullSource);
	public static boolean heap_referenced = false;
	public static Declaration HEAP_START = new Declaration(
													new Token(TokenType.IDENTIFIER, nullSource, "HEAP_START"), 
													new INT(), 
													zero_atom, 
													nullSource);
	
	
	public List<String> referencedLibaries = new ArrayList();
	
	public CompilerDriver(String [] args) {
		this.readConfig();
		this.readArgs(args);
	}
	
	public CompilerDriver() {
		this.readConfig();
	}
	
	public void readConfig() {
		/* Read Configuration */
		List<String> conf = Util.readFile(new File("src\\Snips\\sys-inf.xml"));
		if (conf == null) conf = readFromJar("sys-inf.xml");
		
		sys_config  = new XMLNode(conf);
	}
	
	public String getVersionString() {
		return sys_config.getValue("Version");
	}
	
	public List<String> readFromJar(String path) {
		List<String> lines = new ArrayList();
	    
		try {
			InputStream is = CompilerDriver.class.getResourceAsStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
		    String line;
		    while ((line = br.readLine()) != null)  {
		    	lines.add(line);
		    }
		    br.close();
		    is.close();
		} catch (Exception e) {
			return null;
		}
	    
	    return lines;
	}
	
	public List<String> compile(File file, List<String> code) {
		long start = System.currentTimeMillis();
		
		/* Setup & set settings */
		List<String> output = null;
		LabelGen.reset();
		CompilerDriver.file = file;
		printLogo();
		log.clear();
		
		try {
			if (code == null) {
				throw new SNIPS_EXCEPTION("SNIPS -> Input is null!");
			}
			
			if (imm) {
				log.add(new Message("SNIPS -> Recieved Code:", Message.Type.INFO));
				code.stream().forEach(x -> System.out.println(CompilerDriver.printDepth + x));
			}
			
			log.add(new Message("SNIPS -> Starting compilation.", Message.Type.INFO));
			
					/* --- PRE-PROCESSING --- */
			PreProcessor preProcess = new PreProcessor(code, file.getName());
			List<LineObject> preCode = preProcess.getProcessed();
			
			
					/* --- SCANNING --- */
			log.add(new Message("SNIPS_SCAN -> Starting...", Message.Type.INFO));
			Scanner scanner = new Scanner(preCode);
			List<Token> deque = scanner.scan();
			
			
					/* --- PARSING --- */
			log.add(new Message("SNIPS_PARSE -> Starting...", Message.Type.INFO));
			Parser parser = new Parser(deque);
			SyntaxElement AST = parser.parse();
			
			
					/* --- PROCESS IMPORTS --- */
			Program p = (Program) AST;
			p.fileName = file.getPath();
			if (p.directives.stream().filter(x -> x instanceof IncludeDirective).count() > 0 || !referencedLibaries.isEmpty()) {
				List<SyntaxElement> dependencies = this.addDependencies(p.directives, p.fileName);
				
				/* An error occured during importing, probably loop in dependencies */
				if (dependencies == null) {
					throw new SNIPS_EXCEPTION("SNIPS -> Failed to import libraries, possible loop in #include statements.");
				}
				
				/* Print out imported libaries */
				for (SyntaxElement s : dependencies) {
					log.add(new Message("SNIPS -> Imported library " + ((Program) s).fileName, Message.Type.INFO));
				}
				
				/* Add libaries to AST, duplicates were already filtered */
				int c = 0;
				for (int i = 0; i < dependencies.size(); i++) {
					Program p0 = (Program) dependencies.get(i);
					for (int a = 0; a < p0.programElements.size(); a++) {
						p.programElements.add(c, p0.programElements.get(a));
						c++;
					}
				}
				
				/* Clear program directives */
				p.directives.clear();
			}
			
			
					/* --- CONTEXT CHECKING --- */
			log.add(new Message("SNIPS_CTX -> Starting...", Message.Type.INFO));
			ContextChecker ctx = new ContextChecker(AST);
			ctx.check();
			log.add(new Message("SNIPS_CTX -> Nothing to report.", Message.Type.INFO));
		
			if (imm) AST.print(4, true);
			
			
					/* --- CODE GENERATION --- */
			log.add(new Message("SNIPS_CGEN -> Starting...", Message.Type.INFO));
			AsNBody body = AsNBody.cast((Program) AST);
		
			
					/* --- OPTIMIZING --- */
			if (!disableOptimizer) {
				double before = body.getInstructions().size();
				log.add(new Message("SNIPS_ASMOPT -> Starting...", Message.Type.INFO));
				
				ASMOptimizer opt = new ASMOptimizer();
				opt.optimize(body);
			
				double rate = Math.round(1 / (before / 100) * (before - body.getInstructions().size()) * 100) / 100;
				CompilerDriver.compressions.add(rate);
				log.add(new Message("SNIPS_ASMOPT -> Compression rate: " + rate + "%", Message.Type.INFO));
			}
			
			
					/* --- OUTPUT BUILDING --- */
			output = body.getInstructions().stream().filter(x -> ((x instanceof ASMComment)? enableComments : true)).map(x -> {
				return x.build() + ((x.comment != null && enableComments)? x.comment.build(x.build().length()) : "");
			}).collect(Collectors.toList());
		
			
			if (imm) {
				log.add(new Message("SNIPS -> Outputted Code:", Message.Type.INFO));
				output.stream().forEach(x -> System.out.println(CompilerDriver.printDepth + x));
			}
			
			CompilerDriver.instructionsGenerated += output.size();
		} catch (Exception e) {
			if (printErrors) e.printStackTrace();
		}
		
		/* Report Status */
		int err = this.getMessageTypeNumber(Message.Type.FAIL);
		int warn = this.getMessageTypeNumber(Message.Type.WARN);
		
		/* Compilation finished ... */
		if (err > 0) silenced = false;
		log.add(new Message("Compilation " + 
				/* ... successfully */
				((err == 0 && warn == 0)? "finished successfully in " + (System.currentTimeMillis() - start) + " Millis." : 
				/* ... with errors */
				((err > 0)? "aborted with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + "."), (err == 0)? Message.Type.INFO : Message.Type.FAIL));		
		
		log.clear();
		
		if (outputPath != null && output != null) {
			Util.writeInFile(output, outputPath);
			log.add(new Message("SNIPS -> Saved to file: " + outputPath, Message.Type.INFO));
		}
		
		return output;
	}
	
	/**
	 * Used to hot compile referenced libaries.
	 * @param files The libary files to compile
	 * @return A list of ASTs containing the contents of the files as AST.
	 * @throws SNIPS_EXCEPTION 
	 */
	public List<SyntaxElement> hotCompile(List<String> files) throws SNIPS_EXCEPTION {
		List<SyntaxElement> ASTs = new ArrayList();
		
		for (String filePath : files) {
			for (XMLNode c : sys_config.getNode("Library").children) {
				String [] v = c.value.split(":");
				if (v [0].equals(filePath)) {
					filePath = v [1];
				}
			}
			
			File file = new File(filePath);
			
			/* Read from file */
			List<String> code = Util.readFile(file);
			
			/* Read from jar */
			if (code == null) {
				code = readFromJar(filePath);
			}
			
			/* Libary was not found */
			if (code == null) {
				throw new SNIPS_EXCEPTION("SNIPS -> Failed to locate library " + filePath);
			}
			
			SyntaxElement AST = null;
			
			try {
				PreProcessor preProcess = new PreProcessor(code, file.getName());
				List<LineObject> lines = preProcess.getProcessed();
				
					/* --- SCANNING --- */
				Scanner scanner = new Scanner(lines);
				List<Token> deque = scanner.scan();
				
				
						/* --- PARSING --- */
				Parser parser = new Parser(deque);
				AST = parser.parse();
				((Program) AST).fileName = file.getPath();
				
				
						/* --- PROCESS IMPORTS --- */
				List<SyntaxElement> dependencies = this.addDependencies(((Program) AST).directives, ((Program) AST).fileName);
				
				if (dependencies == null) return null;
				else {
					this.removeDuplicates(dependencies);
					ASTs.addAll(dependencies);
				}
			} catch (Exception e) {
				if (printErrors) e.printStackTrace();
				log.add(new Message("SNIPS -> Failed to import library " + filePath + ".", Message.Type.FAIL));
			}
			
			ASTs.add(AST);
		}
		
		this.removeDuplicates(ASTs);
		
		return ASTs;
	}

	/**
	 * Import depencendies listed by include directives
	 * @param importer The program that lists the include directives
	 * @throws SNIPS_EXCEPTION 
	 */
	public List<SyntaxElement> addDependencies(List<Directive> imports, String fileName) throws SNIPS_EXCEPTION {
		List<SyntaxElement> ASTs = new ArrayList();
		
		for (String s : this.referencedLibaries) {
			List<String> file0 = new ArrayList();
			file0.add(s);
			
			CompilerDriver driver = new CompilerDriver();
			ASTs.addAll(driver.hotCompile(file0));
		}
		
		try {
			for (Directive dir : imports) {
				if (dir instanceof IncludeDirective) {
					IncludeDirective inc = (IncludeDirective) dir;
					
					if (inc.file.equals(fileName)) continue;
					for (int i = 0; i < ASTs.size(); i++) {
						if (((Program) ASTs.get(i)).fileName.equals(inc.file)) {
							continue;
						}
					}
					
					List<String> file0 = new ArrayList();
					file0.add(inc.file);
					
					CompilerDriver driver = new CompilerDriver();
					List<SyntaxElement> dependencies = driver.hotCompile(file0);
					
					/* Error during importing, propagate back */
					if (dependencies == null) return null;
					else {
						ASTs.addAll(dependencies);
					}
				}
			}
		} catch (StackOverflowError st) {
			return null;
		}
		
		this.removeDuplicates(ASTs);
		
		return ASTs;
	}
	
	public void removeDuplicates(List<SyntaxElement> ASTs) {
		if (ASTs.size() > 1) for (int i = 0; i < ASTs.size(); i++) {
			for (int a = i + 1; a < ASTs.size(); a++) {
				Program p0 = (Program) ASTs.get(i);
				Program p1 = (Program) ASTs.get(a);
				
				if (p0.fileName.equals(p1.fileName)) {
					ASTs.remove(a);
					a--;
				}
			}
		}
	}
	
	public int getMessageTypeNumber(Message.Type type) {
		return (int) log.stream().filter(x -> x.messageType == type).count();
	}
	
	public void printLogo() {
		/* Print out all lines of the SNIPS logo */
		if (logoPrinted) return;
		else logoPrinted = true;
		
		for (String s : logo)System.out.println(s);
		
		String ver = "Gen.2 " + getVersionString();
		int l = ver.length();
		for (int i = 0; i < 41 - l; i++) ver = " " + ver;
		ver = "\t" + ver;
		System.out.println(ver);
		System.out.println();
	}
	
	public void readArgs(String [] args) {
		if (args [0].equals("-help")) {
			printHelp();
			System.exit(0);
		}
		else if (args [0].equals("-info")) {
			printInfo();
			System.exit(0);
		}
		
		file = new File(args [0]);
		
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				if (args [i].equals("-viz"))useTerminalColors = false;
				else if (args [i].equals("-imm"))imm = true;
				else if (args [i].equals("-warn"))disableWarnings = true;
				else if (args [i].equals("-opt"))disableOptimizer = true;
				else if (args [i].equals("-com"))enableComments = false;
				else if (args [i].equals("-log")) {
					logoPrinted = false;
					silenced = false;
				}
				else if (args [i].equals("-o")) {
					outputPath = args [i + 1];
					i++;
				}
				else log.add(new Message("Unknown Parameter: " + args [i], Message.Type.FAIL));
			}
		}
		
		if (silenced) logoPrinted = true;
	}
	
	public void printHelp() {
		silenced = false;
		new Message("Arguments: ", Message.Type.INFO);
		System.out.println(CompilerDriver.printDepth + "-info     : Print Version Compiler Version and information");
		System.out.println(CompilerDriver.printDepth + "[Path]    : First argument, set input file");
		System.out.println(CompilerDriver.printDepth + "-log      : Print out log and compile information");
		System.out.println(CompilerDriver.printDepth + "-com      : Remove comments from assembly");
		System.out.println(CompilerDriver.printDepth + "-warn     : Disable Warnings");
		System.out.println(CompilerDriver.printDepth + "-opt      : Disable Optimizer");
		System.out.println(CompilerDriver.printDepth + "-imm      : Print out immediate representations");
		System.out.println(CompilerDriver.printDepth + "-o [Path] : Specify output file");
		System.out.println(CompilerDriver.printDepth + "-viz      : Disable Ansi Color in Log messages");
	}
	
	public void printInfo() {
		silenced = false;
		new Message("Version: Snips Compiler Gen.2 " + getVersionString(), Message.Type.INFO);
	}
	
	public void setBurstMode(boolean value, boolean imm) {
		CompilerDriver.silenced = value;
		CompilerDriver.imm = imm;
		CompilerDriver.printErrors = !value;
	}
	
	public static void printAverageCompression() {
		double [] rate = {0};
		CompilerDriver.compressions.stream().forEach(x -> rate [0] += x / CompilerDriver.compressions.size());
		double r0 = rate [0];
		r0 = Math.round(r0 * 100.0) / 100.0;
		log.add(new Message("SNIPS_ASMOPT -> Average compression rate: " + r0 + "%", Message.Type.INFO));
		log.add(new Message("SNIPS_ASMOPT -> Instructions generated: " + CompilerDriver.instructionsGenerated, Message.Type.INFO));
	}
	
}
