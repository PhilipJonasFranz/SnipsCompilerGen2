package Tst;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import REv.CPU.ProcessorUnit;
import REv.Modules.Tools.XMLParser;
import REv.Modules.Tools.XMLParser.XMLNode;
import Snips.CompilerDriver;
import Util.Util;
import Util.Logging.Message;

public class TestDriver {

			/* --- NESTED --- */
	/** Result summary of a test */
	public class Result {
		
		public RET_TYPE res;
		
		public int succ, fail;
		
		public Result(RET_TYPE res, int succ, int fail) {
			this.res = res;
			this.succ = succ;
			this.fail = fail;
		}
		
	}
	
	/** Test result Status */
	public enum RET_TYPE {	
		SUCCESS, FAIL, CRASH, TIMEOUT
	}
	
	
			/* --- FIELDS --- */
	/** The Milliseconds the program can run on the processor until it timeouts */
	public long ttl = 200;
	
	public boolean detailedCompilerMessages = false;
	
	public boolean displayCompilerImmediateRepresentations = false;
	
	public boolean printResult = false;
	
	
			/* --- METHODS --- */
	public static void main(String [] args) {
		new TestDriver(args);	
	}
	
	public TestDriver(String [] args) {
		/* Setup Compiler Driver */
		CompilerDriver.printLogo();
		CompilerDriver.useTerminalColors = true;
		
		List<String> paths = new ArrayList();
		
		if (args.length == 0) paths.addAll(this.getTestFiles("res\\Test\\").stream().filter(x -> x.endsWith(".txt")).collect(Collectors.toList()));
		else {
			for (String s : args) {
				if (s.endsWith(".sn")) paths.add(s);
				else {
					paths.addAll(this.getTestFiles(s).stream().filter(x -> x.endsWith(".txt")).collect(Collectors.toList()));
				}
			}
		}
		
		if (paths.size() == 0) {
			new Message("Could not find any tests, make sure the path starts from the res/ folder.", Message.Type.WARN);
			new Message("Make sure the test files are .txt files.", Message.Type.WARN);
			System.exit(0);
		}
		
		long start = System.currentTimeMillis();
		int failed = 0, crashed = 0, timeout = 0;
		
		new Message("Starting run, found " + paths.size() + " test" + ((paths.size() == 1)? "" : "s") + ".", Message.Type.INFO);
		
		String current = null;
		for (String file : paths) {
			try {
				/* Read content of test file */
				List<String> content = Util.readFile(new File(file));
				current = file;
			
				/* Extract contents */
				List<String> code = new ArrayList();
				List<String> testcases = new ArrayList();
				
				int i = 0;
				if (content.get(0).equals("DESCRIPTION")) {
					while (!content.get(i).equals("SOURCE")) {
						i++;
					}
					i++;
				}
				else i = 1;
				
				while (true) {
					if (content.get(i).equals("TESTCASES")) {
						i++;
						break;
					}
					code.add(content.get(i));
					i++;
				}
				while (i < content.size()) {
					testcases.add(content.get(i));
					i++;
				}
			
				new Message("Testing file " + file, Message.Type.INFO);
				
				/* Run test */
				Result res = this.test(file, code, testcases);
				
				if (res.fail > 0) failed++;
				else if (res.res == RET_TYPE.CRASH) crashed++;
				else if (res.res == RET_TYPE.TIMEOUT) timeout++;
				else new Message("Test finished successfully.", Message.Type.INFO);
			
			} catch (Exception e) {
				new Message("-> Test " + current + " ran into an error!", Message.Type.FAIL);
				crashed++;
				e.printStackTrace();
			}
		}
		
		new Message("Finished " + paths.size() + " test" + ((paths.size() == 1)? "" : "s") + ((failed == 0 && crashed == 0 && timeout == 0)? " successfully in " + 
				(System.currentTimeMillis() - start) + " Millis" : ", " + failed + " test(s) failed" + 
				((crashed > 0)? ", " + crashed + " tests(s) crashed" : "")) + 
				((timeout > 0)? ", " + timeout + " tests(s) timed out" : "") + ".", 
				(failed == 0 && crashed == 0)? Message.Type.INFO : Message.Type.FAIL);
		
		if (crashed == 0 && timeout == 0 && failed == 0) {
			CompilerDriver.printAverageCompression();
			new Message("[BUILD] Successful.", Message.Type.INFO);
		}
		else {
			new Message("[BUILD] Failed.", Message.Type.FAIL);
		}
	}
	
	@SuppressWarnings("deprecation")
	public Result test(String path, List<String> code, List<String> cases) throws InterruptedException {
		CompilerDriver cd = new CompilerDriver();
		CompilerDriver.driver = cd;
		
		cd.setBurstMode(!this.detailedCompilerMessages, this.displayCompilerImmediateRepresentations);
		
		File file = new File(path);
		List<String> compile = cd.compile(new File(path), code);
		
		cd.setBurstMode(false, false);
		
		if (compile == null) {
			new Message("-> A crash occured during compilation.", Message.Type.FAIL);
			if (this.printResult) new Message("-> Tested code:", Message.Type.FAIL);
			cd.compile(file, code);
			return new Result(RET_TYPE.CRASH, 0, 0);
		}
		
		if (this.printResult) {
			compile.stream().forEach(System.out::println);
		}
		
		int succ = 0;
		int fail = 0;
		
		boolean printedOutput = this.printResult;
		
		/* Setup Runtime Environment */
		for (int i = 0; i < cases.size(); i++) {
			//if (cases.size() > 1) new Message("Running testcase " + (i + 1) + "/" + cases.size(), Message.Type.INFO);
			String [] sp = cases.get(i).split(" ");
			
			boolean assemblyMessages = false;
			XMLNode head = new XMLParser(new File("res\\Test\\config.xml")).root;
			ProcessorUnit pcu = REv.Modules.Tools.Util.buildEnvironmentFromXML(head, compile, !assemblyMessages);
			
			/* Setup parameters in registers and stack */
			if (sp.length > 1) {
				int r = 0;
				int st = 0;
				for (int a = 0; a < sp.length - 1; a++) {
					if (r < 3) pcu.regs [a] = pcu.toBinary(Integer.parseInt(sp [a]));
					else {
						int n = pcu.memoryBlocks.length - 1;
						pcu.memoryBlocks [n][pcu.memoryBlocks [n].length - (st + 1)] = pcu.toBinary(Integer.parseInt(sp [a]));
						pcu.regs [13] = pcu.toBinary(pcu.toDecimal(pcu.regs [13]) - 4);
						st++;
					}
					r++;
				}
			}
			
			/* Execute Program */
			pcu.debug = 0;
			pcu.step = 1;
			
			Thread runThread = new Thread(new Runnable() {
				public void run() {
					pcu.execute();
				}
			});
			
			long start = System.currentTimeMillis();
			runThread.start();
			while (runThread.isAlive()) {
				if (System.currentTimeMillis() - start > ttl) {
					runThread.interrupt();
					runThread.stop();
					runThread = null;
					new Message("The compiled program timed out!", Message.Type.FAIL);
					if (cases.size() > 1) new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", Message.Type.FAIL);
					fail++;
					compile.stream().forEach(x -> System.out.println(x));
				}
			}
			
			int pcu_return = REv.Modules.Tools.Util.toDecimal2K(pcu.regs [0]);
			
			if (pcu_return == Integer.parseInt(sp [sp.length - 1])) {
				/* Output does match expected value */
				succ++;
			}
			else {
				/* Wrong output */
				if (cases.size() > 1) new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", Message.Type.FAIL);
				new Message("-> Expected <" + Integer.parseInt(sp [sp.length - 1]) + ">, actual <" + pcu_return + ">.", Message.Type.FAIL);
				
				/* Print inputted parameters */
				String params = "-> Params: ";
				if (sp.length == 1) params += "-";
				else {
					for (int a = 0; a < sp.length - 1; a++) {
						params += sp [a];
						if (a < sp.length - 2) {
							params += ", ";
						}
					}
				}
				new Message(params, Message.Type.FAIL);
				
				if (!printedOutput) {
					new Message("-> Outputted Assemby Program: ", Message.Type.FAIL);
					compile.stream().forEach(x -> System.out.println(x));
				}
				printedOutput = true;
				
				fail++;
			}
		}
		
		return new Result((fail > 0)? RET_TYPE.FAIL : RET_TYPE.SUCCESS, succ, fail);
	}
	
	/**
	 * Recursiveley get all test files from the res/Test/ directory
	 * @return A list of all file names.
	 */
	public List<String> getTestFiles(String path) {
		try (Stream<Path> walk = Files.walk(Paths.get(path))) {
			List<String> result = walk.filter(Files::isRegularFile)
				.map(x -> x.toString()).collect(Collectors.toList());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
