package Snips;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import CGen.LabelGen;
import CGen.Opt.ASMOptimizer;
import Ctx.ContextChecker;
import Exc.CGEN_EXC;
import Exc.CTX_EXC;
import Exc.PARSE_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.StructTypedef;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Parser;
import Par.Scanner;
import Par.Token;
import Par.Token.TokenType;
import PreP.NamespaceProcessor;
import PreP.PreProcessor;
import PreP.PreProcessor.LineObject;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;
import Util.Util;
import Util.XMLParser.XMLNode;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

public class CompilerDriver {

	public static String [] logo = {
		"	  _______  __    _  ___  _______  _______",
		"	 |       ||  \\  | ||   ||       ||       |",
		"	 |  _____||   \\_| ||   ||    _  ||  _____|",
		"	 | |_____ |       ||   ||   |_| || |_____",
		"	 |_____  ||  _    ||   ||    ___||_____  |",
		"	  _____| || | \\   ||   ||   |     _____| |",
		"	 |_______||_|  \\__||___||___|    |_______|"};
	
	
			/* --- FLAGS & SETTINGS --- */
	public static boolean 
		logoPrinted = false, 
		useTerminalColors = true, 
		silenced = true,
		imm = false,
		enableComments = true,
		disableModifiers = false,
		disableOptimizer = false,
		optimizeFileSize = false,
		disableWarnings = false,
		disableStructSIDHeaders = false,
		includeMetaInformation = true,
		printErrors = false;
			
			/* --- DEBUG --- */
	public static boolean
		printProvisoTypes = false,
		includeProvisoInTypeString = false,
		printObjectIDs = false;
	
			/* --- FORMATTING --- */
	public static String printDepth = "    ";
	public static int commentDistance = 45;
	
			/* --- STATS --- */
	/* Documents the occurred compression rates */
	public static List<Double> compressions = new ArrayList();
	
	/* Documents the minimum and maximum compression reached */
	public static double c_min = 100, c_max = 0;
	
	/* Counts the amount of the different instructions */
	public static HashMap<String, Integer> ins_p = new HashMap();
	
	/* Counts the total sum of all instructions */
	public static int instructionsGenerated = 0;
	
	
			/* --- ACCESSIBILITY --- */
	public static List<Message> log = new ArrayList();
	
	public static File inputFile;
	
	public static String outputPath;

	public static CompilerDriver driver;
	
	public static XMLNode sys_config;
	
	
			/* --- RESERVED DECLARATIONS & RESSOURCES --- */
	public static Source nullSource = new Source("Default", 0, 0);
	public static Atom zero_atom = new Atom(new INT("0"), new Token(TokenType.INTLIT, nullSource), nullSource);
	
	public static boolean null_referenced = false;
	public static Declaration NULL_PTR = new Declaration(new NamespacePath("NULL"), new INT(), zero_atom, MODIFIER.SHARED, nullSource);
	
	public static boolean heap_referenced = false;
	public static Declaration HEAP_START = new Declaration(new NamespacePath("HEAP_START"), new INT(), zero_atom, MODIFIER.SHARED, nullSource);
								
	public static Declaration create(String name, TYPE type) {
		List<String> path1 = new ArrayList();
		path1.add(name);
		NamespacePath pa1 = new NamespacePath(path1);
		
		Declaration dec = new Declaration(pa1, type, MODIFIER.SHARED, nullSource);
		return dec;
	}
													
			/* --- MAIN --- */
	public static void main(String [] args) {
		/* Check if filepath argument was passed */
		if (args.length == 0) {
			System.out.println(new Message("No input file specified! See -help for argument information.", Message.Type.FAIL).getMessage());
			System.exit(0);
		}
		
		/* Instantiate new Compiler instance with filepath as argument */
		CompilerDriver scd = new CompilerDriver(args);
		driver = scd;
		
		/* Output path is not set, use path of input file, and save to out.s */
		if (outputPath == null) {
			outputPath = args [0];
			
			/* Trim input file name from path */
			if (outputPath.contains("\\")) 
				while (!outputPath.endsWith("\\")) outputPath = outputPath.substring(0, outputPath.length() - 1);
			
			outputPath += "out.s";
		}
		
		/* Errors occurred due to faulty parameters, abort */
		if (!log.isEmpty()) {
			log.add(new Message("Aborting.", Message.Type.FAIL));
			log.stream().forEach(x -> System.out.println(x.getMessage()));
			log.clear();
			System.exit(0);
		}
		
		/* Read code from input file... */
		List<String> code = Util.readFile(inputFile);
		
		/* ...and compile! */
		scd.compile(inputFile, code);
	}
	
	
			/* --- FIELDS --- */
	/** 
	 * Dynamic libraries referenced in the program, like resv or __op_div. 
	 * These will be included in second stage import resolving.
	 */
	public List<String> referencedLibaries = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
	/** Default constructor */
	public CompilerDriver(String [] args) {
		this.readConfig();
		this.readArgs(args);
	}
	
	/** Used for debug purposes */
	public CompilerDriver() {
		this.readConfig();
	}
	
	
			/* --- METHODS --- */
	public void readConfig() {
		/* Read Configuration */
		List<String> conf = Util.readFile(new File("src\\Snips\\sys-inf.xml"));
		if (conf == null) conf = readFromJar("sys-inf.xml");
		
		sys_config  = new XMLNode(conf);
	}
	
	/**
	 * Attempts to read from the .jar of the compiler, with
	 * replative path to the CompilerDriver.class
	 * @param path The path to the file in the jar relative to the class.
	 * @return The contents of the given file or null.
	 */
	public List<String> readFromJar(String path) {
		List<String> lines = new ArrayList();
	    
		try {
			InputStream is = CompilerDriver.class.getResourceAsStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String line;
		    while ((line = br.readLine()) != null) lines.add(line);

		    br.close();
		    is.close();
		} catch (Exception e) {
			return null;
		}
	    
	    return lines;
	}
	
	public List<String> compile(File file0, List<String> code) {
		long start = System.currentTimeMillis();
		
		/* Setup & set settings */
		List<String> output = null;
		LabelGen.reset();
		inputFile = file0;
		printLogo();
		log.clear();
		
		try {
			/* Recieved no source code */
			if (code == null) 
				throw new SNIPS_EXC("SNIPS -> Input is null!");
			
			if (imm) {
				log.add(new Message("SNIPS -> Recieved Code:", Message.Type.INFO));
				code.stream().forEach(x -> System.out.println(printDepth + x));
			}
			
			log.add(new Message("SNIPS -> Starting compilation.", Message.Type.INFO));
			
					/* --- PRE-PROCESSING --- */
			PreProcessor preProcess = new PreProcessor(code, inputFile.getName());
			List<LineObject> preCode = preProcess.getProcessed();
			
			
					/* --- SCANNING --- */
			ProgressMessage scan_progress = new ProgressMessage("SCAN -> Starting", 30, Message.Type.INFO);
			Scanner scanner = new Scanner(preCode, scan_progress);
			List<Token> deque = scanner.scan();
			
			
					/* --- PARSING --- */
			ProgressMessage parse_progress = new ProgressMessage("PARS -> Starting", 30, Message.Type.INFO);
			Parser parser = new Parser(deque, parse_progress);
			SyntaxElement AST = parser.parse();
			
			
					/* --- PROCESS IMPORTS --- */
			Program p = (Program) AST;
			p.fileName = inputFile.getPath();
			if (!referencedLibaries.isEmpty()) {
				List<Program> dependencies = this.addDependencies();
				
				/* Print out imported libaries */
				for (SyntaxElement s : dependencies) 
					log.add(new Message("PRE1 -> Imported library " + ((Program) s).fileName, Message.Type.INFO));
				
				/* Add libaries to AST, duplicates were already filtered */
				int c = 0;
				for (Program p0 : dependencies) {
					for (SyntaxElement s : p0.programElements) 
						p.programElements.add(c++, s);
				}
			}
			
			
					/* --- NAMESPACE MANAGER --- */
			NamespaceProcessor nameProc = new NamespaceProcessor();
			nameProc.process((Program) AST);
			
			if (imm) AST.print(4, true);
			
			
					/* --- CONTEXT CHECKING --- */
			ProgressMessage ctx_progress = new ProgressMessage("CTEX -> Starting", 30, Message.Type.INFO);
			ContextChecker ctx = new ContextChecker(AST, ctx_progress);
			ctx.check();
		
			if (imm) AST.print(4, true);
			
			
					/* --- CODE GENERATION --- */
			ProgressMessage cgen_progress = new ProgressMessage("CGEN -> Starting", 30, Message.Type.INFO);
			AsNBody body = AsNBody.cast((Program) AST, cgen_progress);

			/* Remove comments left over by removed functions */
			for (int i = 1; i < body.instructions.size(); i++) {
				if (body.instructions.get(i) instanceof ASMSeperator && body.instructions.get(i - 1) instanceof ASMComment) {
					body.instructions.remove(i - 1);
					i -= 2;
					if (i < 1) i = 1;
				}
			}
			
			
					/* --- OPTIMIZING --- */
			if (!disableOptimizer) {
				double before = body.getInstructions().size();
				ProgressMessage aopt_progress = new ProgressMessage("OPT1 -> Starting", 30, Message.Type.INFO);
				
				ASMOptimizer opt = new ASMOptimizer();
				opt.optimize(body);
			
				aopt_progress.incProgress(1);
				
				double rate = Math.round(1 / (before / 100) * (before - body.getInstructions().size()) * 100) / 100;
				compressions.add(rate);
				
				if (rate < c_min) c_min = rate;
				if (rate > c_max) c_max = rate;
				
				log.add(new Message("OPT1 -> Compression rate: " + rate + "%", Message.Type.INFO));
			}
			
			
					/* --- OUTPUT BUILDING --- */
			output = new ArrayList();
			
			/* Build the output as a list of strings. Filter comments out if comments are disabled, and count instruction types. */
			output = body.getInstructions().stream().filter(x -> ((x instanceof ASMComment)? enableComments : true)).map(x -> {
				
				/* Count instruction types */
				if (ins_p.containsKey(x.getClass().getName())) ins_p.replace(x.getClass().getName(), ins_p.get(x.getClass().getName()) + 1);
				else ins_p.put(x.getClass().getName(), 1);
				
				/* Build instruction with or without comment */
				return x.build() + ((x.comment != null && enableComments)? x.comment.build(x.build().length()) : "");
			}).collect(Collectors.toList());
		
			/* Remove double empty lines */
			for (int i = 1; i < output.size(); i++) {
				if (output.get(i - 1).trim().equals("") && output.get(i).trim().equals("")) 
					output.remove(i-- - 1);
			}
			
			if (imm) {
				log.add(new Message("SNIPS -> Outputted Code:", Message.Type.INFO));
				output.stream().forEach(x -> System.out.println(printDepth + x));
			}
			
			instructionsGenerated += output.size();
		
		} catch (Exception e) {
			boolean customExc = (e instanceof CGEN_EXC) || (e instanceof CTX_EXC) || (e instanceof PARSE_EXC) || (e instanceof SNIPS_EXC);
			
			/* Exception is not ordinary and internal, print message and stack trace */
			if (!customExc) log.add(new Message("An unexpected error has occurred:", Message.Type.FAIL));
			if (printErrors || !customExc) e.printStackTrace();
			if (!customExc) log.add(new Message("Please contact the developer and include the input file if possible.", Message.Type.FAIL));
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
	 * @throws SNIPS_EXC 
	 */
	public List<Program> hotCompile(List<String> files) throws SNIPS_EXC {
		List<Program> ASTs = new ArrayList();
		
		for (String filePath : files) {
			for (XMLNode c : sys_config.getNode("Library").children) {
				String [] v = c.value.split(":");
				if (v [0].equals(filePath)) 
					filePath = v [1];
			}
			
			File file = new File(filePath);
			
			/* Read from file */
			List<String> code = Util.readFile(file);
			
			if (code == null) 
				code = Util.readFile(new File("release\\" + filePath));
			
			/* Libary was not found */
			if (code == null) 
				throw new SNIPS_EXC("SNIPS -> Failed to locate library " + filePath);
			
			Program AST = null;
			
			try {
					/* --- PRE-PROCESS --- */
				PreProcessor preProcess = new PreProcessor(code, file.getName());
				List<LineObject> lines = preProcess.getProcessed();
				
				
					/* --- SCANNING --- */
				Scanner scanner = new Scanner(lines, null);
				List<Token> deque = scanner.scan();
				
				
						/* --- PARSING --- */
				Parser parser = new Parser(deque, null);
				AST = (Program) parser.parse();
				AST.fileName = file.getPath();
				
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
	 * Import dependencies of dynamic imports
	 * @throws SNIPS_EXC 
	 */
	public List<Program> addDependencies() throws SNIPS_EXC {
		List<Program> ASTs = new ArrayList();
		
		for (String s : this.referencedLibaries) {
			List<String> file0 = new ArrayList();
			file0.add(s);
			
			CompilerDriver driver = new CompilerDriver();
			ASTs.addAll(driver.hotCompile(file0));
		}
		
		this.removeDuplicates(ASTs);
		
		return ASTs;
	}
	
	public void removeDuplicates(List<Program> ASTs) {
		if (ASTs.size() > 1) for (int i = 0; i < ASTs.size(); i++) {
			for (int a = i + 1; a < ASTs.size(); a++) {
				if (ASTs.get(i).fileName.equals(ASTs.get(a).fileName)) {
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
		if (logoPrinted) return;
		else logoPrinted = true;
		
		for (String s : logo) System.out.println(s);
		
		String ver = "Gen.2 " + sys_config.getValue("Version");
		
		int l = ver.length();
		for (int i = 0; i < 41 - l; i++) ver = " " + ver;
		
		System.out.println("\t" + ver + "\n");
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
		
		inputFile = new File(args [0]);
		
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				if (args [i].equals("-viz"))useTerminalColors = false;
				else if (args [i].equals("-imm"))imm = true;
				else if (args [i].equals("-warn"))disableWarnings = true;
				else if (args [i].equals("-opt"))disableOptimizer = true;
				else if (args [i].equals("-ofs"))optimizeFileSize = true;
				else if (args [i].equals("-com"))enableComments = false;
				else if (args [i].equals("-rov"))disableModifiers = true;
				else if (args [i].equals("-sid"))disableStructSIDHeaders = true;
				else if (args [i].equals("-log")) {
					logoPrinted = false;
					silenced = false;
				}
				else if (args [i].equals("-o")) outputPath = args [i++ + 1];
				else log.add(new Message("Unknown Parameter: " + args [i], Message.Type.FAIL));
			}
		}
		
		if (silenced) logoPrinted = true;
	}
	
			/* --- CONSOLE INFORMATION --- */
	public void printHelp() {
		silenced = false;
		new Message("Arguments: ", Message.Type.INFO);
		
		String [] params = {
				"-info     : Print Version Compiler Version and information",
				"[Path]    : First argument, set input file",
				"-log      : Print out log and compile information",
				"-com      : Remove comments from assembly",
				"-warn     : Disable Warnings",
				"-opt      : Disable Optimizer",
				"-ofs      : Optimize for Filesize, slight performance hit",
				"-rov      : Disable visibility modifiers",
				"-sid      : Disable SID headers, lower memory usage, but no instanceof",
				"-imm      : Print out immediate representations",
				"-o [Path] : Specify output file",
				"-viz      : Disable Ansi Color in Log messages"
		};
	
		for (String s : params) System.out.println(printDepth + s);
	}
	
	public void printInfo() {
		silenced = false;
		new Message("Version: Snips Compiler Gen.2 " + sys_config.getValue("Version"), Message.Type.INFO);
	}
	
	
			/* --- DEBUG --- */
	public void setBurstMode(boolean value, boolean imm0) {
		silenced = value;
		imm = imm0;
		printErrors = !value;
	}
	
	public static void printAverageCompression() {
		double [] rate = {0};
		compressions.stream().forEach(x -> rate [0] += x / compressions.size());
		double r0 = rate [0];
		r0 = Math.round(r0 * 100.0) / 100.0;
		
		String f = "  ";
		
		if (!disableOptimizer) {
			log.add(new Message("SNIPS_OPT1 -> Compression Statistics: ", Message.Type.INFO));
			
			/* Plot compression statistics */		
			System.out.println();
			
			int [] map = new int [100];
			for (double d : compressions) {
				map [(int) d - 1]++;
			}
			
			int m = 0;
			for (int i : map) if (i > m) m = i;
			
			f = ("" + m).replaceAll(".", " ");
			
			for (int i = m; i >= 0; i--) {
				
				if (i % 5 == 0) {
					String num = "" + i;
					for (int k = 0; k < f.length() - num.length(); k++) System.out.print(" ");
					System.out.print(num + "|");
				}
				else System.out.print(f + "|");
				for (int a = 0; a < 100; a++) {
					if (map [a] > i) System.out.print("\u2588");
					else System.out.print(" ");
				}
				System.out.println();
			}
			
			for (int i = 0; i < 100; i++) {
				if (i > f.length()) System.out.print("-");
				else System.out.print(" ");
			}
			System.out.println();
			
			System.out.print(" ");
			String s = f;
			for (int i = 0; i <= 100; i += 10) {
				if (i % 10 == 0) {
					s += "" + i;
					while (s.length() < i + 10) s += " ";
				}
			}
			
			System.out.println(s + "\n");
			
			log.add(new Message("SNIPS_OPT1 -> Average compression rate: " + r0 + "%, min: " + c_min + "%, max: " + c_max + "%", Message.Type.INFO));
		}
		
		log.add(new Message("SNIPS_OPT1 -> Relative frequency of instructions: ", Message.Type.INFO));
		
		List<Pair<Integer, String>> rmap = new ArrayList();
		for (Entry<String, Integer> e : ins_p.entrySet()) {
			if (rmap.isEmpty()) {
				rmap.add(new Pair<Integer, String>(e.getValue(), e.getKey()));
			}
			else {
				boolean added = false;
				for (int i = 0; i < rmap.size(); i++) {
					if (e.getValue() > rmap.get(i).first) {
						rmap.add(i, new Pair<Integer, String>(e.getValue(), e.getKey()));
						added = true;
						break;
					}
				}
				
				if (!added) rmap.add(new Pair<Integer, String>(e.getValue(), e.getKey()));
			}
		}
		
		if (!rmap.isEmpty()) {
			System.out.println();
			
			double stretch = 1.0;
			
			if (rmap.get(0).first > 75) {
				stretch = 75.0 / rmap.get(0).first;
			}
			
			for (int i = 0; i < rmap.size(); i++) {
				System.out.print(f + "|");
				for (int a = 0; a < (int) ((double) rmap.get(i).first * stretch); a++) {
					System.out.print("\u2588");
				}
				
				String n = rmap.get(i).second.split("\\.") [rmap.get(i).second.split("\\.").length - 1];
				
				System.out.println(" : " + n + " (" + rmap.get(i).first + ")");
			}
			
			System.out.println();
		}
		
		log.add(new Message("SNIPS_OPT1 -> Total Instructions generated: " + Util.formatNum(instructionsGenerated), Message.Type.INFO));
	}
	
	/** Resets flags during burst compilation */
	public static void reset() {
		heap_referenced = false;
		null_referenced = false;
	}
	
}
