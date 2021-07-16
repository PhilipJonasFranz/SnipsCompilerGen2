package Snips;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Exc.*;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Directive.ASMDirective;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.AST.Expression.Atom;
import Imm.AST.Program;
import Imm.AST.Statement.Declaration;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNBody;
import Imm.TYPE.PRIMITIVES.INT;
import Lnk.Linker;
import Lnk.Linker.LinkerUnit;
import Lnt.Linter;
import Opt.ASM.ASMOptimizer;
import Opt.AST.ASTOptimizer;
import Par.Parser;
import Par.Scanner;
import Par.Token;
import PreP.NamespaceProcessor;
import PreP.PreProcessor;
import PreP.PreProcessor.LineObject;
import Res.Manager.FileUtil;
import Res.Manager.RessourceManager;
import Res.Manager.TranslationUnit;
import SEEn.SEEngine;
import Util.*;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;
import XMLParser.MalformedXMLException;
import XMLParser.XMLParser;
import XMLParser.XMLParser.XMLNode;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class CompilerDriver {

	
	public static String [] logo = {
		"	  _______  __    _  ___  _______  _______",
		"	 |       ||  \\  | ||   ||       ||       |",
		"	 |  _____||   \\_| ||   ||    _  ||  _____|",
		"	 | |_____ |       ||   ||   |_| || |_____",
		"	 |_____  ||  _    ||   ||    ___||_____  |",
		"	  _____| || | \\   ||   ||   |     _____| |",
		"	 |_______||_|  \\__||___||___|    |_______|"};
	
	
			/* ---< FLAGS & SETTINGS >--- */
	public static boolean 
		saveLog = 						false,  /* Everything written to the log is saved in a file 			*/
		logoPrinted = 					false, 	/* Set to true when the logo was printed once. 					*/
		useTerminalColors = 			true, 	/* ANSI-Escape codes are used in the console. 					*/
		silenced = 						true,	/* Less or no messages are printed to the console. 				*/
		imm = 							false,	/* Immediates like the AST are printed.							*/
		out = 							false,	/* Print final output.											*/
		enableComments = 				true,	/* The compiler adds and preserves comments in the output. 		*/
		disableModifiers = 				false,	/* Modifier violations are ignored.								*/
		useASTOptimizer	= 				true,	/* Wether to use the AST optimizer in the pipeline.				*/
		useASMOptimizer = 				true,	/* The optimizer modules are skipped in the pipeline.			*/
		useExperimentalOptimizer =		false,	/* If true, new and potentially buggy opt features are used.	*/
		optimizeFileSize = 				false,	/* The optimizer attempts to minimize the output size. 			*/
		enableSEEn = 					false,	/* Enables the SE-Verifiction System SEEn.			 			*/
		disableWarnings = 				false,	/* No warnings are printed.										*/
		useLinter = 					false,	/* The linter pipeline stage is run.							*/
		buildObjectFileOnly = 			false,	/* Builds the object file only and adds include directives.		*/
		buildModulesRecurse = 			false,	/* Builds all modules in the input and saves them.				*/
		pruneModules = 					false,	/* Prune all exisiting modules and start from scratch.			*/
		printAllImports = 				false,	/* Print out all imported libraries during pre-processing 		*/
		printStats = 					false,	/* Print out optimizer statistics.						 		*/
		linkOnly = 						false;	/* Only link the given input							 		*/
		

			/* --- DEBUG --- */
	public static boolean
		printProvisoTypes = 			false,	/* Print out proviso types when generating type string.			*/
		printObjectIDs = 				false,	/* Print object IDs when generating type strings.				*/
		printErrors = 					false,	/* Print stacktraces, used for debug. 							*/
		expectError =					false,	/* Expect an error during compilation, used for debug. 			*/
		useDefaultVersionID = 			true;   /* Set to false to prevent .version directive.					*/
	
	
			/* ---< FORMATTING --- */
	public static String printDepth = "    ";	/* Inserted in front of every ASM instruction in output.		*/
	public static int commentDistance = 45;		/* How far comments are formated into the output form the left.	*/
	
	
			/* --- STATS >--- */
	public enum PIPE_STAGE {
		PREP("Pre-Processor"),
		SCAN("Scanner"),
		PARS("Parser"),
		IMPM("Import Manager"),
		NAMM("Namespace Manager"),
		CTEX("Context Checker"),
		LINT("Linter"),
		SEEN("SE-Verification"),
		OPT0("AST Optimizer"),
		CGEN("Code Generation"),
		OPT1("ASM Optimizer");
		
		String name;
		
		PIPE_STAGE(String name) {
			this.name = name;
		}
		
	}
	
	/** Print stream all debug and log messages are printed to */
	public static BufferedPrintStream outs = new BufferedPrintStream(System.out);
	
	/* Documents the occurred compression rates */
	public static List<Double> compressions1 = new ArrayList();
	
	/* Documents the minimum and maximum compression reached */
	public static double c_min1 = 100, c_max1 = 0;
	
	/* Documents the occurred compression rates */
	public static List<Double> compressions0 = new ArrayList();
	
	/* Documents the minimum and maximum compression reached */
	public static double c_min0 = 100, c_max0 = 0;
	
	public static int opt0_loops = 0, opt0_exc = 0;
	
	/* Counts the amount of the different instructions */
	public static HashMap<String, Integer> ins_p = new HashMap();
	
	/* Counts the total sum of all instructions */
	public static int instructionsGenerated = 0;
	
	public static Stack<SyntaxElement> stackTrace;
	
	
			/* ---< ACCESSIBILITY --- */
	public static List<Message> log = new ArrayList();
	
	public static File inputFile;
	
	public static String outputPath;

	public static CompilerDriver driver;
	
	public static XMLNode sys_config;
	
	public static XMLNode metrics_config;
	
	public static HashMap<String, Pair<Integer, Integer>> node_metrics = new HashMap();
	
	public Exception thrownException = null;
	
	public static PIPE_STAGE currentStage = null;
	
	
			/* --- RESERVED DECLARATIONS & RESSOURCES >--- */
	public static Source nullSource = new Source("Default", 0, 0);
	public static Atom zero_atom = new Atom(new INT("0"), nullSource);
	
	public static boolean null_referenced = false;
	public static Declaration NULL_PTR = new Declaration(new NamespacePath("NULL"), new INT(), zero_atom, MODIFIER.SHARED, nullSource);
	
	public static boolean heap_referenced = false;
	public static Declaration HEAP_START = new Declaration(new NamespacePath("HEAP_START"), new INT(), zero_atom, MODIFIER.SHARED, nullSource);
								
	
			/* ---< MAIN --- */
	public static void main(String [] args) {
		/* Check if filepath argument was passed */
		if (args.length == 0) {
			CompilerDriver.outs.println(new Message("No input file specified! See -help for argument information.", LogPoint.Type.FAIL).getMessage());
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
			log.add(new Message("Aborting.", LogPoint.Type.FAIL));
			log.forEach(x -> CompilerDriver.outs.println(x.getMessage()));
			log.clear();
			System.exit(0);
		}
		
		/* Read code from input file... */
		List<String> code = FileUtil.readFile(inputFile);
		
		/* ...and compile! */
		scd.compile(inputFile, code);
	}
	
	
			/* ---< FIELDS >--- */
	/** 
	 * Dynamic libraries referenced in the program, like resv or __op_div. 
	 * These will be included in second stage import resolving.
	 */
	public List<String> referencedLibaries = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/** Default constructor */
	public CompilerDriver(String [] args) {
		this.readConfig();
		this.readArgs(args);
	}
	
	/** Used for debug purposes */
	public CompilerDriver() {
		this.readConfig();
	}
	
	public SyntaxElement createOPT0Dupe(List<Token> dequeue) throws PARS_EXC, CTEX_EXC {
		boolean silenced = CompilerDriver.silenced;
		CompilerDriver.silenced = true;
		
		SyntaxElement AST = STAGE_PARS(dequeue);
		STAGE_PRE1(AST);
		STAGE_NAME(AST);
		STAGE_CTEX(AST);

		CompilerDriver.silenced = silenced;
		return AST;
	}
	
	
			/* ---< METHODS >--- */
	public List<String> compile(File file0, List<String> code) {
		long start = System.currentTimeMillis();
		
		/* Setup & set settings */
		List<String> output = null;
		LabelUtil.reset();
		inputFile = file0;
		printLogo();
		log.clear();
		
		try {
			/* Recieved no source code */
			if (code == null) throw new SNIPS_EXC("SNIPS -> Input file not found! Make sure to use absolute filepaths.");
			
			if (imm) {
				log.add(new Message("SNIPS -> Recieved Code:", LogPoint.Type.INFO));
				code.forEach(x -> CompilerDriver.outs.println(printDepth + x));
			}
			
			if (!linkOnly) {
				
				log.add(new Message("SNIPS -> Starting compilation.", LogPoint.Type.INFO));
				
						/* ---< PRE-PROCESSING >--- */
				List<LineObject> preCode = STAGE_PREP(code);
				
				if (imm) {
					log.add(new Message("SNIPS -> Pre-Processed Code:", LogPoint.Type.INFO));
					preCode.forEach(x -> CompilerDriver.outs.println(printDepth + x.line));
				}
				
						/* ---< SCANNING >--- */
				List<Token> dequeue = STAGE_SCAN(preCode);
				
				/* If needed, create a cache of the tokens here */
				List<Token> dupeCache = new ArrayList();
				if (CompilerDriver.useASTOptimizer)
					dupeCache.addAll(dequeue);
				
						/* ---< PARSING >--- */
				SyntaxElement AST = STAGE_PARS(dequeue);
				
						/* --- PROCESS DYNAMIC IMPORTS >--- */
				STAGE_PRE1(AST);

				/* ---< NAMESPACE MANAGER >--- */
				STAGE_NAME(AST);

				if (imm) AST.print(4, true);
				
						/* ---< CONTEXT CHECKING >--- */
				STAGE_CTEX(AST);

				/* ---< LINTER >--- */
				if (useLinter) STAGE_LINT(AST);

				/* ---< LINTER >--- */
				if (enableSEEn) STAGE_SEEN((Program) AST);

				if (imm) AST.print(4, true);
				
						/* ---< AST OPTIMIZER >--- */
				if (useASTOptimizer) {
					List<String> ref = new ArrayList(driver.referencedLibaries);
					driver.referencedLibaries.clear();
					STAGE_OPT0(AST, this.createOPT0Dupe(dupeCache));
					driver.referencedLibaries = ref;
				}
				
				if (imm) AST.print(4, true);
				
						/* ---< CODE GENERATION >--- */
				AsNBody body = STAGE_CGEN(AST);
	
						/* ---< ASM OPTIMIZER >--- */
				STAGE_OPT1(body);

				/* ---< OUTPUT BUILDING >--- */
				output = this.buildOutput(body.originUnit, false);
			
						/* ---< BUILD AND DUMP MODULES >--- */
				STAGE_DUMP(AsNBody.translationUnits);
				
			}
			else output = code;
			
			
					/* ---< LINKING >--- */
			output = STAGE_LINK(output);
			
			
			if (imm || out) {
				log.add(new Message("SNIPS -> Outputted Code:", LogPoint.Type.INFO));
				output.forEach(x -> CompilerDriver.outs.println(printDepth + x));
			}
			
			/* Error test generated instructions are not counted, since they duplicate many times. */
			if (!expectError) instructionsGenerated += output.size();
		
		} catch (Exception e) {
			boolean customExc = e instanceof CGEN_EXC || e instanceof CTEX_EXC || 
								e instanceof PARS_EXC || e instanceof LINK_EXC || 
								e instanceof OPT0_EXC || e instanceof SNIPS_EXC;
			
			/* Exception is not ordinary and internal, print message and stack trace */
			if (printErrors || !customExc) {
				if (!customExc) {
					log.add(new Message("An unexpected error has occurred:", LogPoint.Type.FAIL));
					
					log.add(new Message("Please contact the developer and include the input file if possible.", LogPoint.Type.FAIL));
					
					/* Give rough estimate where error occurred */
					
					String approx = "Pipeline Stage: " + currentStage.name;
					
					if (stackTrace != null && !stackTrace.isEmpty()) 
						approx += ", at location estimate: " + stackTrace.peek().getSource().getSourceMarker();
					else
						approx = approx.substring(0, approx.length() - 2);
					
					log.add(new Message(approx, LogPoint.Type.FAIL));
				}
				
				e.printStackTrace();
			}
			
			this.thrownException = e;
			if (expectError) return null;
		}
		
		/* Report Status */
		int err = (int) log.stream().filter(x -> x.messageType == Type.FAIL).count();
		int warn = (int) log.stream().filter(x -> x.messageType == Type.WARN).count();
		
		/* Compilation finished ... */
		if (err > 0) silenced = false;
		log.add(new Message("Operation " + 
				/* ... successfully */
				((err == 0 && warn == 0)? "finished successfully in " + (System.currentTimeMillis() - start) + " Millis." : 
				/* ... with errors */
				((err > 0)? "aborted with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + "."), (err == 0)? LogPoint.Type.INFO : LogPoint.Type.FAIL));		
		
		log.clear();
		
		if (printStats) {
			Util.printOPT0Graph(ASTOptimizer.complexity);
			Util.printStats();
		}
		
		if (outputPath != null && output != null) {
			FileUtil.writeInFile(output, outputPath);
			log.add(new Message("SNIPS -> Saved to file: " + outputPath, LogPoint.Type.INFO));
		}
		
		if (saveLog) {
			outs.flush();
			
			List<String> out = outs.getContents();
			
			String path = CompilerDriver.outputPath;
			String [] sp = path.replace('\\', '/').split("/");
			path = path.substring(0, path.length() - sp [sp.length - 1].length());
			
			FileUtil.writeInFile(out, path + "compile.log");
		}
		
		return output;
	}
	
	public List<String> buildOutput(TranslationUnit unit, boolean isModule) {
		
		List<ASMInstruction> unitBuild = unit.buildTranslationUnit();
		List<String> output = new ArrayList();
		
		boolean isMainFile = unit.sourceFile.equals(RessourceManager.instance.toASMPath(inputFile.getPath()));
		
		if (unit.hasVersionChanged() || isMainFile || pruneModules) {
			
			/* 
			 * The version has changed, this means that all exsisting module assembly has become invalid.
			 * So we have to delete all existing asssembly and re-build the module from scratch.
			 */
			
			if (pruneModules)
				new Message("SNIPS -> Pruned module '" + unit.sourceFile + "'", Type.INFO);
			
			if (!isMainFile && !pruneModules)
				new Message("SNIPS -> Module changed: '" + unit.sourceFile + "'", Type.INFO);
			
			/* Build the output as a list of strings. Filter comments out if comments are disabled, and count instruction types. */
			output = unitBuild.stream().filter(x -> (!(x instanceof ASMComment) || (!isModule && enableComments))).map(x -> {
				
				/* Count instruction types */
				if (ins_p.containsKey(x.getClass().getName())) ins_p.replace(x.getClass().getName(), ins_p.get(x.getClass().getName()) + 1);
				else ins_p.put(x.getClass().getName(), 1);
				
				String ins_build = x.build();

				/* Build instruction with or without comment */
				return ins_build + ((x.comment != null && enableComments)? ASMComment.build(x.comment, ins_build.length()) : "");
			}).collect(Collectors.toList());
		
			/* Remove double empty lines */
			for (int i = 1; i < output.size(); i++) {
				if (output.get(i - 1).trim().equals("") && output.get(i).trim().equals("")) 
					output.remove(i-- - 1);
			}
		}
		else {
			
			/* 
			 * Version has not changed, this means we can keep all existing assembly source.
			 * This means we have to merge the new assembly with the existing module unit.
			 */
			
			int changes = 0;
			
			LinkerUnit existingUnit = unit.existingUnit;
			
			/* Output is patched existing unit */
			output = existingUnit.build();
			
			/* Imports */
			existingUnit.imports.clear();
			existingUnit.imports.addAll(unit.imports);
			
			/* Data Section */
			existingUnit.dataSection.clear();
			for (ASMInstruction dataIns : unit.dataSection)
				existingUnit.dataSection.add(dataIns.build());
			
			/* Text Section */
			for (int i = 0; i < unitBuild.size(); i++) {
				ASMInstruction ins = unitBuild.get(i);
				 
				if (ins instanceof ASMDirective dir) {
					if (dir.hardCode.startsWith(".global")) {
						/* Found function head */
						boolean found = false;
						for (String s : output) {
							if (s.equals(dir.hardCode)) {
								found = true;
								break;
							}
						}
						
						if (!found) {
							changes++;
							
							output.add("");
							
							for (int a = i; a < unitBuild.size(); a++) {
								ASMInstruction ins0 = unitBuild.get(a);
								
								if (!ins0.equals(dir) && ins0 instanceof ASMDirective dir0) {
									if (dir0.hardCode.startsWith(".global")) {
										i = a - 1;
										break;
									}
								}
								else output.add(ins0.build());
							}
						}
					}
				}
			}
			
			if (changes > 0)
				new Message("SNIPS -> Updating module '" + unit.sourceFile + "', made " + changes + " change" + ((changes > 1)? "s" : ""), Type.INFO);
			
		}
		
		return output;
	}
	
	/**
	 * Used to hot compile referenced libaries.
	 * @param files The libary files to compile
	 * @return A list of ASTs containing the contents of the files as AST.
	 */
	public List<Program> hotCompile(List<String> files) throws SNIPS_EXC {
		ArrayList ASTs = new ArrayList();
		
		for (String filePath : files) {
			
			File file = new File(RessourceManager.instance.resolve(filePath));
			List<String> code = RessourceManager.instance.getFile(filePath);
			
			/* Libary was not found */
			if (code == null) 
				throw new SNIPS_EXC("SNIPS -> Failed to locate library " + filePath);
			
			Program AST = null;
			
			try {
					/* --- PRE-PROCESS >--- */
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
				log.add(new Message("SNIPS -> Failed to import library " + filePath + ".", LogPoint.Type.FAIL));
			}
			
			ASTs.add(AST);
		}
		
		Util.removeDuplicates(ASTs);
		return ASTs;
	}

	/**
	 * Import dependencies of dynamic imports
	 */
	public List<Program> addDependencies() throws SNIPS_EXC {
		List<Program> ASTs = new ArrayList();
		
		/* Create a copy of all referenced libraries, all transitive dependencies are already included. */
		String [] referenced = this.referencedLibaries.toArray(String[]::new);
		
		for (String s : referenced) {
			List<String> file0 = new ArrayList();
			file0.add(s);
			
			CompilerDriver driver = new CompilerDriver();
			ASTs.addAll(driver.hotCompile(file0));
		}
		
		Util.removeDuplicates(ASTs);
		return ASTs;
	}
	
	
			/* ---< COMPILER PIPELINE STAGES >--- */
	private static List<LineObject> STAGE_PREP(List<String> codeIn) {
		currentStage = PIPE_STAGE.PREP;
		PreProcessor preProcess = new PreProcessor(codeIn, inputFile.getPath());
		List<LineObject> preCode = preProcess.getProcessed();
		if (CompilerDriver.buildModulesRecurse) 
			new Message("Recompiling " + PreProcessor.modulesIncluded + " modules", Type.INFO);
		
		return preCode;
	}
	
	private static List<Token> STAGE_SCAN(List<LineObject> code) {
		currentStage = PIPE_STAGE.SCAN;
		ProgressMessage scan_progress = new ProgressMessage("SCAN -> Starting", 30, LogPoint.Type.INFO);
		Scanner scanner = new Scanner(code, scan_progress);
		List<Token> dequeue = scanner.scan();
		scan_progress.finish();
		return dequeue;
	}
	
	private static SyntaxElement STAGE_PARS(List<Token> dequeue) throws PARS_EXC {
		currentStage = PIPE_STAGE.PARS;
		ProgressMessage parse_progress = new ProgressMessage("PARS -> Starting", 30, LogPoint.Type.INFO);
		Parser parser = new Parser(dequeue, parse_progress);
		SyntaxElement AST = parser.parse();
		parse_progress.finish();
		return AST;
	}

	private static SyntaxElement STAGE_PRE1(SyntaxElement AST) {
		currentStage = PIPE_STAGE.IMPM;
		Program p = (Program) AST;
		p.fileName = inputFile.getPath();
		if (!driver.referencedLibaries.isEmpty()) {
			List<Program> dependencies = driver.addDependencies();
			
			/* Print out imported libaries */
			if (printAllImports)
				for (Program s : dependencies)
					log.add(new Message("PRE1 -> Imported library " + s.fileName, LogPoint.Type.INFO));
			
			/* Add libaries to AST, duplicates were already filtered */
			int c = 0;
			for (Program p0 : dependencies) {
				for (SyntaxElement s : p0.programElements) 
					p.programElements.add(c++, s);
			}
		}
		
		return AST;
	}
	
	private static SyntaxElement STAGE_NAME(SyntaxElement AST) {
		currentStage = PIPE_STAGE.NAMM;
		NamespaceProcessor nameProc = new NamespaceProcessor();
		nameProc.process((Program) AST);
		return AST;
	}
	
	private static SyntaxElement STAGE_CTEX(SyntaxElement AST) throws CTEX_EXC {
		currentStage = PIPE_STAGE.CTEX;
		ProgressMessage ctx_progress = new ProgressMessage("CTEX -> Starting", 30, LogPoint.Type.INFO);
		ContextChecker ctx = new ContextChecker(AST, ctx_progress);
		ctx.check();
		ctx_progress.finish();
		return AST;
	}
	
	private static SyntaxElement STAGE_LINT(SyntaxElement AST) {
		currentStage = PIPE_STAGE.LINT;
		ProgressMessage lnt_progress = new ProgressMessage("LINT -> Starting", 30, LogPoint.Type.INFO);
		Linter lnt = new Linter((Program) AST);
		lnt.lint();
		lnt_progress.finish();
		lnt.report();
		return AST;
	}

	private static SyntaxElement STAGE_SEEN(Program AST) throws PARS_EXC {
		currentStage = PIPE_STAGE.SEEN;
		ProgressMessage seen_progress = new ProgressMessage("SEEN -> Starting", 30, LogPoint.Type.INFO);
		SEEngine seEngine = new SEEngine(AST);
		seEngine.interpret();

		seen_progress.finish();

		seEngine.report.forEach(x -> System.out.println(x.getMessage()));

		return AST;
	}
	
	private static SyntaxElement STAGE_OPT0(SyntaxElement AST, SyntaxElement AST0) {
		if (useASTOptimizer) {
			double nodes_before = AST.visit(x -> true).size();
			currentStage = PIPE_STAGE.OPT0;
			ProgressMessage opt_progress = new ProgressMessage("OPT0 -> Starting", 30, LogPoint.Type.INFO);
			ASTOptimizer opt0 = new ASTOptimizer();
			AST = opt0.optProgram((Program) AST, (Program) AST0);
			opt_progress.finish();
			
			if (imm) AST.codePrint(0).forEach(CompilerDriver.outs::println);
			
			double nodes_after = AST.visit(x -> true).size();
			
			double rate = Math.round(1 / (nodes_before / 100) * (nodes_before - nodes_after) * 100) / 100.0;
			
			if (!expectError) {
				compressions0.add(rate);
			
				if (rate < c_min0) c_min0 = rate;
				if (rate > c_max0) c_max0 = rate;
			}
			
			opt0_loops += opt0.CYCLES; opt0_exc++;
			new Message("OPT0 -> Optimization Cycles: " + opt0.CYCLES + ", Nodes: " + nodes_before + " -> " + nodes_after, LogPoint.Type.INFO);
		}
		
		return AST;
	}
	
	private static AsNBody STAGE_CGEN(SyntaxElement AST) throws CGEN_EXC, CTEX_EXC {
		currentStage = PIPE_STAGE.CGEN;
		ProgressMessage cgen_progress = new ProgressMessage("CGEN -> Starting", 30, LogPoint.Type.INFO);
		AsNBody body = AsNBody.cast((Program) AST, cgen_progress);
		
		/* Remove comments left over by removed functions */
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMSeperator && body.instructions.get(i - 1) instanceof ASMComment) {
				body.instructions.remove(i - 1);
				i -= 2;
				if (i < 1) i = 1;
			}
		}
		
		cgen_progress.finish();
		return body;
	}
	
	private static AsNBody STAGE_OPT1(AsNBody body) {
		if (useASMOptimizer) {
			currentStage = PIPE_STAGE.OPT1;
			
			double rate = new ASMOptimizer().optimize(body.instructions, true);
			
			for (Entry<String, TranslationUnit> entry : AsNBody.translationUnits.entrySet()) 
				rate += new ASMOptimizer().optimize(entry.getValue().textSection, false);
			
			rate /= AsNBody.translationUnits.size();
			
			rate = Math.round(rate * 100) / 100.0;
			
			if (!expectError) {
				compressions1.add(rate);
			
				if (rate < c_min1) c_min1 = rate;
				if (rate > c_max1) c_max1 = rate;
			}
			
			log.add(new Message("OPT1 -> Compression rate: " + rate + "%", LogPoint.Type.INFO));
		}
		
		return body;
	}
	
	private static void STAGE_DUMP(HashMap<String, TranslationUnit> translationUnits) {
		if (buildModulesRecurse) {
			for (Entry<String, TranslationUnit> entry : translationUnits.entrySet()) {
				String path = RessourceManager.instance.resolve(entry.getKey());

				/* Build main file seperately */
				String excludeMainPath = RessourceManager.instance.toASMPath(inputFile.getPath());
				if (entry.getKey().equals(excludeMainPath)) continue;
				
				List<String> module = driver.buildOutput(entry.getValue(), true);
				
				boolean write = FileUtil.writeInFile(module, path);
				if (!write) log.add(new Message("SNIPS -> Failed to resolve module path: " + entry.getKey(), LogPoint.Type.WARN));
			}
		}
	}
	
	private static List<String> STAGE_LINK(List<String> asm) throws LINK_EXC {
		if (!CompilerDriver.buildObjectFileOnly) {
			Linker.link_progress = new ProgressMessage("LINK -> Starting", 30, LogPoint.Type.INFO);
			LinkerUnit originUnit = Linker.parseLinkerUnit(asm);
			Lnk.Linker.linkProgram(new ArrayList(), originUnit);
			asm = originUnit.build();
			Linker.link_progress.finish();
			Linker.buffer.forEach(Message::flush);
		}
		
		return asm;
	}
	
	
			/* ---< CONSOLE INFORMATION --- */
	public void printLogo() {
		if (logoPrinted) return;
		else logoPrinted = true;
		
		for (String s : logo) CompilerDriver.outs.println(s);
		
		String ver = "Gen.2 " + sys_config.getValue("Version");
		
		int l = ver.length();
		for (int i = 0; i < 41 - l; i++) ver = " " + ver;
		
		CompilerDriver.outs.println("\t" + ver + "\n");
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
				if (args [i].equals("-viz"))		useTerminalColors = false;
				else if (args [i].equals("-imm")) 	imm = true;
				else if (args [i].equals("-stat")) 	printStats = true;
				else if (args [i].equals("-warn")) 	disableWarnings = true;
				else if (args [i].equals("-lnt")) 	useLinter = true;
				else if (args [i].equals("-imp")) 	printAllImports = true;
				else if (args [i].equals("-opt0"))  useASTOptimizer = false;
				else if (args [i].equals("-opt1"))  useASMOptimizer = false;
				else if (args [i].equals("-opt1e")) useExperimentalOptimizer = false;
				else if (args [i].equals("-opt")) {
					useASTOptimizer = false;
					useASMOptimizer = false;
				}
				else if (args [i].equals("-ofs")) 	optimizeFileSize = true;
				else if (args [i].equals("-v")) 	enableSEEn = true;
				else if (args [i].equals("-com")) 	enableComments = false;
				else if (args [i].equals("-rov")) 	disableModifiers = true;
				else if (args [i].equals("-O")) 	buildObjectFileOnly = true;
				else if (args [i].equals("-r")) 	buildModulesRecurse = true;
				else if (args [i].equals("-R")) {
													buildModulesRecurse = true;
													pruneModules = true;
				}
				else if (args [i].equals("-L")) 	linkOnly = true;
				else if (args [i].equals("-F")) {
					List<String> flags = new ArrayList();
					i++;
					while (i < args.length && !args [i].startsWith("-")) {
						flags.add(args [i]);
						i++;
					}
					
					PreProcessor.passedFlags = flags;
				}
				else if (args [i].equals("-log")) {
													logoPrinted = false;
													silenced = false;
				}
				else if (args [i].equals("-logs")) {
													logoPrinted = false;
													silenced = false;
													saveLog = true;
				}
				else if (args [i].equals("-o")) 	outputPath = args [i++ + 1];
				else log.add(new Message("Unknown Parameter: " + args [i], LogPoint.Type.FAIL));
			}
		}
		
		if (silenced) logoPrinted = true;
	}
	
	public void printHelp() {
		silenced = false;
		new Message("Arguments: ", LogPoint.Type.INFO);
		
		String [] params = {
				"-info     : Print Version Compiler Version and information",
				"[Path]    : First argument, set input file",
				"-log      : Print out log and compile information",
				"-logs     : Print out log and compile information, save log to file",
				"-com      : Remove comments from assembly",
				"-warn     : Disable warnings",
				"-lnt      : Find potential issues in the code, print out warnings.",
				"-imp      : Print out all imports",
				"-opt0     : Disable AST Optimizer",
				"-opt1     : Disable ASM Optimizer",
				"-opt      : Disable code optimizers",
				"-ofs      : Optimize for filesize, slight performance penalty",
				"-v        : Enable Symbolic-Execution Verification Pass",
				"-rov      : Disable visibility modifiers",
				"-O        : Build object file only, required additional linking",
				"-r        : Re-build all changed required modules and save them",
				"-R        : Force to re-build all required modules and save them",
				"-L        : Link the given assembly file. Requires the input file to be a .s file.",
				"-F        : Pass define flags to the PreProcessor to use in #ifdef directives.",
				"-imm      : Print out immediate representations",
				"-stat     : Print out statistics",
				"-o [Path] : Specify output file",
				"-viz      : Disable Ansi Color in Log messages"
		};
	
		for (String s : params) CompilerDriver.outs.println(printDepth + s);
	}
	
	public void printInfo() {
		silenced = false;
		new Message("Version: Snips Compiler Gen.2 " + sys_config.getValue("Version"), LogPoint.Type.INFO);
	}
	
	
			/* --- RESSOURCES --- */
	public void readConfig() {
		/* Read Configuration */
		try {
			/* Load library config */
			List<String> sysconf = FileUtil.readFile(new File("release\\sys-inf.xml"));
			if (sysconf == null) sysconf = FileUtil.readFile(new File("sys-inf.xml"));
			sys_config  = XMLParser.parse(sysconf);
			
			/* Load node-to-instruction ratio config */
			List<String> meconf = FileUtil.readFile(new File("release\\metric-inf.xml"));
			if (meconf == null) meconf = FileUtil.readFile(new File("metric-inf.xml"));
			metrics_config  = XMLParser.parse(meconf);
			
			/* Pre-Cache Ratios */
			for (XMLNode child : metrics_config.getChildren()) {
				String [] sp = child.getValue().split(" ");
				Pair<Integer, Integer> ic = new Pair(Integer.parseInt(sp [0]), Integer.parseInt(sp [1]));
				node_metrics.put(child.getID(), ic);
			}
		} catch (MalformedXMLException e) {
			new Message("Failed to parse system configuration!", Type.FAIL);
			System.exit(0);
		}
		
		/* Create list of aliases for system library */
		XMLNode library = new XMLNode("Library");

		List<String> paths = FileUtil.fileWalk("release/lib/")
				.stream().filter(x -> !x.endsWith(".s")).collect(Collectors.toList());

		paths.forEach(x -> {
			/* Cut off 'release/' from path */
			x = x.substring(8);
			
			File file = new File(x);
			
			XMLNode alias = new XMLNode("Alias");
			alias.setValue((file.getName() + ":" + x).replace("\\", "/"));
			
			library.getChildren().add(alias);
		});
		
		sys_config.getChildren().add(library);
	}


	/* --- DEBUG --- */
	public void setBurstMode(boolean value, boolean imm0) {
		silenced = value;
		imm = imm0;
		printErrors = !value;
	}
	
	/** Resets flags during burst compilation */
	public static void reset() {
		heap_referenced = false;
		null_referenced = false;
		expectError = false;
		
		LabelUtil.c.clear();
		
		AsNBody.translationUnits.clear();
		
		ASTOptimizer.complexity.clear();
		
		PreProcessor.importsPerFile.clear();
		PreProcessor.passedFlags.clear();
		PreProcessor.imports.clear();
		PreProcessor.modulesIncluded = 0;
	}
	
} 
