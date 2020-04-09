package Snips;


import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import CGen.LabelGen;
import CGen.Opt.ASMOptimizer;
import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.Structural.ASMComment;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Directive.Directive;
import Imm.AST.Directive.IncludeDirective;
import Imm.AsN.AsNBody;
import Par.Parser;
import Par.Scanner;
import Util.Logging.Message;

public class CompilerDriver {

	/* The Input File */
	public static File file;
	
	public static String [] logo = {
			"	 _______  __    _  ___  _______  _______   ",
			"	|       ||  \\  | ||   ||       ||       | ",
			"	|  _____||   \\_| ||   ||    _  ||  _____| ",
			"	| |_____ |       ||   ||   |_| || |_____   ",
			"	|_____  ||  _    ||   ||    ___||_____  |  ",
			"	 _____| || | \\   ||   ||   |     _____| | ",
			"	|_______||_|  \\__||___||___|    |_______| ",
			"    		                            Gen.2  "};
	
	public static List<Message> log = new ArrayList();
	
	/* Flags & Settings */
	public static boolean 
		logoPrinted = false, 
		useTerminalColors = true, 
		silenced = true,
		imm = false,
		enableComments = true;
	
	public static String outputPath;
	
	public static boolean printErrors = false;
	
	public static String printDepth = "    ";
	public static int commentDistance = 25;
	
	public static List<Double> compressions = new ArrayList();
	
	public static int instructionsGenerated = 0;
	
	public static CompilerDriver driver;
	
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
			System.out.println(new Message("No output file specified! See -help for argument information.", Message.Type.FAIL).getMessage());
			System.exit(0);
		}
		
		/* Errors occurred due to faulty parameters, abort */
		if (!log.isEmpty()) {
			log.add(new Message("Aborting.", Message.Type.FAIL));
			log.stream().forEach(x -> System.out.println(x.getMessage()));
			log.clear();
			System.exit(0);
		}
		
		List<String> code = Util.Util.readFile(file);
		
		/* Perform compilation */
		scd.compile(file, code);
	}
	
	
	public List<String> referencedLibaries = new ArrayList();
	
	public CompilerDriver(String [] args) {
		this.readArgs(args);
	}
	
	public CompilerDriver() {
		
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
			
			
					/* --- SCANNING --- */
			log.add(new Message("SNIPS_SCAN -> Starting...", Message.Type.INFO));
			Scanner scanner = new Scanner(code);
			Deque deque = scanner.scan();
			
			
					/* --- PARSING --- */
			log.add(new Message("SNIPS_PARSE -> Starting...", Message.Type.INFO));
			Parser parser = new Parser(deque);
			SyntaxElement AST = parser.parse();
			
			/* --- PROCESS IMPORTS --- */
			Program p = (Program) AST;
			p.fileName = file.getPath();
			if (p.directives.stream().filter(x -> x instanceof IncludeDirective).count() > 0 || !referencedLibaries.isEmpty()) {
				List<SyntaxElement> dependencies = this.addDependencies(p);
				
				/* An error occured during importing, probably loop in dependencies */
				if (dependencies == null) {
					throw new SNIPS_EXCEPTION("SNIPS -> Failed to import libaries, possible loop in #include statements.");
				}
				
				/* Print out imported libaries */
				for (SyntaxElement s : dependencies) {
					log.add(new Message("SNIPS -> Imported libary " + ((Program) s).fileName, Message.Type.INFO));
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
			try {
				ctx.check();
				log.add(new Message("SNIPS_CTX -> Nothing to report.", Message.Type.INFO));
			} catch (CTX_EXCEPTION e) {
				throw e;
			}
			
			if (imm) AST.print(4, true);
			
			
					/* --- CODE GENERATION --- */
			log.add(new Message("SNIPS_CGEN -> Starting...", Message.Type.INFO));
			AsNBody body = AsNBody.cast((Program) AST);
		
			
					/* --- OPTIMIZING --- */
			double before = body.getInstructions().size();
			log.add(new Message("SNIPS_ASMOPT -> Starting...", Message.Type.INFO));
			ASMOptimizer opt = new ASMOptimizer();
			opt.optimize(body);
			
			double rate = Math.round(1 / (before / 100) * (before - body.getInstructions().size()) * 100) / 100;
			CompilerDriver.compressions.add(rate);
			log.add(new Message("SNIPS_ASMOPT -> Compression rate: " + rate + "%", Message.Type.INFO));
			
			
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
			Util.Util.writeInFile(output, outputPath);
			log.add(new Message("SNIPS -> Saved to file: " + outputPath, Message.Type.INFO));
		}
		
		return output;
	}
	
	/**
	 * Used to hot compile referenced libaries.
	 * @param files The libary files to compile
	 * @return A list of ASTs containing the contents of the files as AST.
	 */
	public List<SyntaxElement> hotCompile(List<String> files) {
		List<SyntaxElement> ASTs = new ArrayList();
		
		for (String filePath : files) {
			File file = new File(filePath);
			List<String> code = Util.Util.readFile(file);
			
			SyntaxElement AST = null;
			
			try {
				if (code == null) 
					throw new SNIPS_EXCEPTION("SNIPS -> Input is null!");
				
				
						/* --- SCANNING --- */
				Scanner scanner = new Scanner(code);
				Deque deque = scanner.scan();
				
				
						/* --- PARSING --- */
				Parser parser = new Parser(deque);
				AST = parser.parse();
				((Program) AST).fileName = file.getPath();
				
				
						/* --- PROCESS IMPORTS --- */
				List<SyntaxElement> dependencies = this.addDependencies((Program) AST);
				
				if (dependencies == null) return null;
				else {
					this.removeDuplicates(dependencies);
					ASTs.addAll(dependencies);
				}
			} catch (Exception e) {
				if (printErrors) e.printStackTrace();
				log.add(new Message("SNIPS -> Failed to import libary " + filePath + ".", Message.Type.FAIL));
			}
			
			ASTs.add(AST);
		}
		
		this.removeDuplicates(ASTs);
		
		return ASTs;
	}

	/**
	 * Import depencendies listed by include directives
	 * @param importer The program that lists the include directives
	 */
	public List<SyntaxElement> addDependencies(Program importer) {
		List<Directive> imports = importer.directives;
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
					
					if (inc.file.equals(importer.fileName)) continue;
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
	
	public static void printLogo() {
		/* Print out all lines of the SNIPS logo */
		if (logoPrinted) return;
		else logoPrinted = true;
		
		for (String s : logo)System.out.println(s);
		System.out.println();
	}
	
	public void readArgs(String [] args) {
		if (args [0].equals("-help")) {
			printHelp();
			System.exit(0);
		}
		
		file = new File(args [0]);
		
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				if (args [i].equals("-viz"))useTerminalColors = false;
				else if (args [i].equals("-imm"))imm = true;
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
		System.out.println(CompilerDriver.printDepth + "[Path]    : First argument, set input file");
		System.out.println(CompilerDriver.printDepth + "-log      : Print out log and compile information");
		System.out.println(CompilerDriver.printDepth + "-com      : Remove comments from assembly");
		System.out.println(CompilerDriver.printDepth + "-imm      : Print out immediate representations");
		System.out.println(CompilerDriver.printDepth + "-o [Path] : Specify output file");
		System.out.println(CompilerDriver.printDepth + "-viz      : Disable Ansi Color in Log messages");
	}
	
	public void setBurstMode(boolean value, boolean imm) {
		CompilerDriver.silenced = value;
		CompilerDriver.imm = imm;
		CompilerDriver.printErrors = !value;
	}
	
	public static void printAverageCompression() {
		double [] rate = {0};
		CompilerDriver.compressions.stream().forEach(x -> rate [0] += x / CompilerDriver.compressions.size());
		double r0 = Math.round(rate [0] * 100) / 100;
		log.add(new Message("SNIPS_ASMOPT -> Average compression rate: " + r0 + "%", Message.Type.INFO));
		log.add(new Message("SNIPS_ASMOPT -> Instructions generated: " + CompilerDriver.instructionsGenerated, Message.Type.INFO));
	}
	
}
