package Snips;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Util.Logging.Message;

public class CompilerDriver {

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
	
	public static boolean logoPrinted = false, useTerminalColors = true, fOut = false;
	
	public static boolean silenced = true, imm = false;
	
	public static void printLogo() {
		if (logoPrinted)return;
		else logoPrinted = true;
		
		for (String s : logo)System.out.println(s);
		System.out.println();
	}
	
	public static void main(String [] args) {
		/* Check if filepath argument was passed */
		if (args.length == 0) {
			System.out.println(new Message("No input file specified!", Message.Type.FAIL).getMessage());
			System.exit(0);
		}
		
		/* Instantiate new Compiler instance with filepath as argument */
		CompilerDriver scd = new CompilerDriver(new File(args [0]));
		
		/* ---- Read and process arguments ---- */
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
	
	public File file;
	
	public List<String> code;
	
	public CompilerDriver(File file) {
		this.file = file;
	}
	
	public CompilerDriver(File file, List<String> code) {
		this.file = file;
		this.code = code;
	}

	public List<String> compile(boolean silenced, boolean imm) {
		printLogo();
		log.clear();
		
		try {
			List<String> output = null;
			
			long start = System.currentTimeMillis();
			
			log.add(new Message("SNIPS -> Starting compilation.", Message.Type.INFO));
			
			/* TODO: Build and insert compilation pipeline modules here */
			
			log.add(new Message("SNIPS -> Missing modules, aborting.", Message.Type.FAIL));
			
			int err = this.getMessageTypeNumber(Message.Type.FAIL);
			int warn = this.getMessageTypeNumber(Message.Type.WARN);
			log.add(new Message("Compilation " + 
					/* Finished successfully */
					((err == 0 && warn == 0)? "finished successfully in " + (System.currentTimeMillis() - start) + " Millis." : 
					/* With errors */
					((err > 0)? "aborted with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + ".\n"), (err == 0)? Message.Type.INFO : Message.Type.FAIL));
			
			if (!silenced)log.stream().forEach(x -> System.out.println(x.getMessage()));
			
			return output;
		} catch (Exception e) {
			e.printStackTrace();
			int err = this.getMessageTypeNumber(Message.Type.FAIL);
			int warn = this.getMessageTypeNumber(Message.Type.WARN);
			log.add(new Message("Compilation aborted " + ((err == 0 && warn == 0)? "" : ((err > 0)? "with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + "."), (err == 0)? Message.Type.INFO : Message.Type.FAIL));	
			if (!silenced) {
				log.stream().forEach(x -> System.out.println(x.getMessage()));
			}
			return null;
		}
	}
	
	public int getMessageTypeNumber(Message.Type type) {
		return (int) log.stream().filter(x -> x.messageType == type).count();
	}
	
}
