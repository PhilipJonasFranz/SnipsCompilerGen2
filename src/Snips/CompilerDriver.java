package Snips;


import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNBody;
import Par.Parser;
import Par.Scanner;
import Util.Logging.Message;

public class CompilerDriver {

	/* The Input File */
	public static File file;
	
	/* The Contents of the input file */
	public List<String> code;
	
	public static String [] logo = {
			"	 _______  __    _  ___  _______  _______  ",
			"	|       ||  |  | ||   ||       ||       | ",
			"	|  _____||   |_| ||   ||    _  ||  _____| ",
			"	| |_____ |       ||   ||   |_| || |_____  ",
			"	|_____  ||  _    ||   ||    ___||_____  | ",
			"	 _____| || | |   ||   ||   |     _____| | ",
			"	|_______||_|  |__||___||___|    |_______| ",
			"    		                   Compiler v.0.1 "};
	
	public static List<Message> log = new ArrayList();
	
	/* Flags & Settings */
	public static boolean 
		logoPrinted = false, 
		useTerminalColors = true, 
		fOut = false,
		silenced = false,
		imm = false;
	
	public static String printDepth = "    ";
	
	
	public static void main(String [] args) {
		/* Check if filepath argument was passed */
		if (args.length == 0) {
			System.out.println(new Message("No input file specified!", Message.Type.FAIL).getMessage());
			System.exit(0);
		}
		
		/* Instantiate new Compiler instance with filepath as argument */
		CompilerDriver scd = new CompilerDriver(args);
		
		/* Errors occurred due to faulty parameters, abort */
		if (!log.isEmpty()) {
			log.add(new Message("Aborting.", Message.Type.FAIL));
			log.stream().forEach(x -> System.out.println(x.getMessage()));
			log.clear();
			System.exit(0);
		}
		
		/* Perform compilation */
		scd.compile(false, imm);
	}
	
	public CompilerDriver(String [] args) {
		this.readFlags(args);
	}
	
	public CompilerDriver(File file, List<String> code) {
		file = file;
		this.code = code;
	}

	public List<String> compile(boolean silenced, boolean imm) {
		long start = System.currentTimeMillis();
		
		printLogo();
		log.clear();
		
		List<String> output = null;
		
		try {
			/* Read in code */
			this.code = Util.Util.readFile(file);
			
			log.add(new Message("SNIPS -> Starting compilation.", Message.Type.INFO));
			
			log.add(new Message("SNIPS_SCAN -> Starting...", Message.Type.INFO));
			Scanner scanner = new Scanner(this.code);
			Deque deque = scanner.scan();
			
			log.add(new Message("SNIPS_PARSE -> Starting...", Message.Type.INFO));
			Parser parser = new Parser(deque);
			SyntaxElement AST = parser.parse();
			
			AST.print(0, true);
			
			log.add(new Message("SNIPS_CTX -> Starting...", Message.Type.INFO));
			ContextChecker ctx = new ContextChecker(AST);
			try {
				ctx.check();
				log.add(new Message("SNIPS_CTX -> Nothing to report.", Message.Type.INFO));
			} catch (CTX_EXCEPTION e) {
				
			}
			
			log.add(new Message("SNIPS_CGEN -> Starting...", Message.Type.INFO));
			AsNBody body = AsNBody.cast((Program) AST);
			
			List<ASMInstruction> build = body.getInstructions();
			List<String> program = build.stream().map(x -> x.build()).collect(Collectors.toList());
		
			System.out.println();
			program.stream().forEach(System.out::println);
			
			/* TODO: Build and insert compilation pipeline modules here */
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/* Report Status */
		int err = this.getMessageTypeNumber(Message.Type.FAIL);
		int warn = this.getMessageTypeNumber(Message.Type.WARN);
		
		/* Compilation finished ... */
		log.add(new Message("Compilation " + 
				/* ... successfully */
				((err == 0 && warn == 0)? "finished successfully in " + (System.currentTimeMillis() - start) + " Millis." : 
				/* ... with errors */
				((err > 0)? "aborted with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + ".\n"), (err == 0)? Message.Type.INFO : Message.Type.FAIL));		
		if (!silenced)log.stream().forEach(x -> System.out.println(x.getMessage()));
		
		return output;
	}
	
	public int getMessageTypeNumber(Message.Type type) {
		return (int) log.stream().filter(x -> x.messageType == type).count();
	}
	
	/* Print out all lines of the SNIPS logo */
	public static void printLogo() {
		if (logoPrinted) return;
		else logoPrinted = true;
		
		for (String s : logo)System.out.println(s);
		System.out.println();
	}
	
	public void readFlags(String [] args) {
		file = new File(args [0]);
		
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				if (args [i].equals("-viz"))useTerminalColors = false;
				else if (args [i].equals("-imm"))imm = true;
				else if (args [i].equals("-o")) {
					fOut = true;
				}
				else log.add(new Message("Unknown Parameter: " + args [i], Message.Type.FAIL));
			}
		}
	}
	
}
