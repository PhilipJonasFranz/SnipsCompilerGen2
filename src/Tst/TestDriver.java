package Tst;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Exc.CGEN_EXC;
import Exc.CTX_EXC;
import Exc.LNK_EXC;
import Exc.PARSE_EXC;
import Exc.SNIPS_EXC;
import Lnk.Linker;
import REv.CPU.ProcessorUnit;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Util;
import Util.XMLParser;
import Util.XMLParser.XMLNode;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.Logging.SimpleMessage;

public class TestDriver {

			/* ---< NESTED >--- */
	/** Result summary of a test */
	public class Result {
		
				/* ---< FIELDS >--- */
		/** The return type status of the test */
		public RET_TYPE res;
		
		/* The amount of test cases that succeded and that failed. */
		public int succ, fail;
		
		
				/* ---< CONSTRUCTORS >--- */
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
	
	
			/* ---< FIELDS >--- */
	/** The amount of milliseconds the program can run on the processor until it counts as a timeout */
	public long ttl = 200, progressIndicatorSpeed = 1000;
	
	/** Print the compiler messages for each test. */
	public boolean detailedCompilerMessages = false;
	
	/** Print the compiler immediate representations */
	public boolean displayCompilerImmediateRepresentations = false;
	
	/** Print the assembly compilation results */
	public boolean printResult = false;
	
	/** Store/Update asm results in the tested file */
	public boolean writebackResults = true;
	
	/** The Result Stack used to propagate package test results back up */
	Stack<ResultCnt> resCnt = new Stack();
	
	public int progress = 0, amount;
	
	public long totalCPUCycles = 0;
	
	public long start, firstStart;
	

			/* ---< CONSTRUCTORS >--- */
	public TestDriver(String [] args) {
		/* Setup Compiler Driver */
		CompilerDriver comp = new CompilerDriver();
		comp.printLogo();
		CompilerDriver.useTerminalColors = true;
		CompilerDriver.silenced = false;
		CompilerDriver.includeMetaInformation = false;
		
		/* Experimental Flags */
		//CompilerDriver.optimizeFileSize = true;
		
		List<String> paths = new ArrayList();
		
		/* Add add files to the paths list */
		if (args.length == 0) paths.addAll(this.getTestFiles("res\\Test\\").stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
		else {
			for (String s : args) {
				if (s.endsWith(".sn")) paths.add(s);
				else {
					paths.addAll(this.getTestFiles(s).stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
				}
			}
		}
		
		amount = paths.size();
		
		/* No paths were found, print warning and quit */
		if (paths.size() == 0) {
			new Message("Could not find any tests, make sure the path starts from the res/ folder.", LogPoint.Type.WARN);
			new Message("Make sure the test files are .txt files.", LogPoint.Type.WARN);
			System.exit(0);
		}
		
		/* Setup Test Node Tree */
		new Message("Starting run, found " + Util.formatNum(paths.size()) + " test" + ((paths.size() == 1)? "" : "s") + ".", LogPoint.Type.INFO);
		TestNode head = new TestNode(paths);
		new Message("Successfully built package tree.", LogPoint.Type.INFO);
		
		start = System.currentTimeMillis();
		firstStart = start;
		
		/* Base Layer */
		resCnt.push(new ResultCnt());
		
		start = System.currentTimeMillis();
		
		/* Test the main package */
		testPackage(head);
		
		/* Print out ASM-OPT stats */
		CompilerDriver.printAverageCompression();
		
		/* Get result and print feedback */
		ResultCnt res = resCnt.pop();
		new Message("Finished " + paths.size() + " test" + ((paths.size() == 1)? "" : "s") + ((res.getFailed() == 0 && res.getCrashed() == 0 && res.getTimeout() == 0)? " successfully in " + 
				Util.formatNum((System.currentTimeMillis() - firstStart)) + " Millis" : ", " + res.getFailed() + " test(s) failed" + 
				((res.getCrashed() > 0)? ", " + res.getCrashed() + " tests(s) crashed" : "")) + 
				((res.getTimeout()> 0)? ", " + res.getTimeout() + " tests(s) timed out" : "") + ".", 
				(res.getFailed() == 0 && res.getCrashed() == 0 && res.getTimeout() == 0)? LogPoint.Type.INFO : LogPoint.Type.FAIL);
		
		new Message("Total CPU Cycles: " + Util.formatNum(totalCPUCycles), LogPoint.Type.INFO);
		
		/* Print Build status */
		if (res.getCrashed() == 0 && res.getTimeout() == 0 && res.getFailed() == 0) {
			new Message("[BUILD] Successful.", LogPoint.Type.INFO);
		}
		else new Message("[BUILD] Failed.", LogPoint.Type.FAIL);
	}
	
	
			/* ---< METHODS >--- */
	/** Launch a new test driver run with given arguments. */
	public static void main(String [] args) {
		new TestDriver(args);	
	}
	
	/**
	 * Tests the package defined by given node as well as all child packages of the node.
	 */
	public void testPackage(TestNode node) {
		boolean buffered = (!detailedCompilerMessages && !displayCompilerImmediateRepresentations && !printResult) || node.tests.isEmpty();
		
		Message headMessage = new Message("Testing Package " + node.getPackagePath(), LogPoint.Type.INFO, buffered);
		boolean printedHead = false;
		
		/* Push summary for package */
		resCnt.push(new ResultCnt());
		
		/* Test all child nodes */
		if (!node.childs.isEmpty()) {
			for (TestNode node0 : node.childs) 
				testPackage(node0);
		}
		
		/* Run all tests in package */
		for (Pair<String, List<Message>> test : node.tests) {
			resCnt.push(new ResultCnt());
			
			/* Run test and save test feedback in pair */
			test.getSecond().addAll(runTest(node.getPackagePath() + test.getFirst()));
			progress++;
			
			/* Get result */
			ResultCnt res = resCnt.pop();
			if (res.getTimeout() > 0 || res.getFailed() > 0 || res.getCrashed() > 0) {
				if (!printedHead) {
					headMessage.flush();
					printedHead = true;
				}
				
				CompilerDriver.silenced = false;
				test.getSecond().stream().forEach(x -> x.flush());
			}
			
			/* Add result to package results */
			resCnt.peek().add(res);
		}
		
		/* Get package results */
		ResultCnt res = resCnt.pop();
		if ((res.getTimeout() > 0 || res.getCrashed() > 0 || res.getFailed() > 0) && !node.tests.isEmpty()) 
			new Message("Package Tests failed: " + node.getPackagePath(), LogPoint.Type.FAIL);
		
		if (System.currentTimeMillis() - start > progressIndicatorSpeed) {
			new Message("Progress: " + Util.formatNum(progress) + "/" + Util.formatNum(amount) + " test(s), total time: " + Util.formatNum((System.currentTimeMillis() - firstStart)) + " ms", LogPoint.Type.INFO);
			start = System.currentTimeMillis();
		}
		
		/* Add package results to super package results */
		resCnt.peek().add(res);
	}
	
	public List<Message> runTest(String file) {
		boolean buffered = !(detailedCompilerMessages || displayCompilerImmediateRepresentations || printResult);
		
		List<Message> buffer = new ArrayList();
		try {
			/* Read content of test file */
			List<String> content = Util.readFile(new File(file));
		
			/* Extract contents */
			List<String> code = new ArrayList();
			List<String> testcases = new ArrayList();
			List<String> writeback = new ArrayList();
			
			/* Extract contents out of the file */
			int i = 0;
			if (content.get(0).equals("DESCRIPTION")) {
				while (!content.get(i).equals("SOURCE")) {
					i++;
				}
				i++;
			}
			else i = 1;
			
			boolean errorTest = false;
			
			while (true) {
				if (content.get(i).equals("TESTCASES") || content.get(i).equals("THROWN")) {
					if (content.get(i).equals("THROWN"))
						errorTest = true;
					
					i++;
					break;
				}
				
				code.add(content.get(i));
				i++;
			}
			
			List<Pair<String, List<String>>> thrown = null;
			
			if (errorTest) {
				thrown = new ArrayList();
				
				List<String> copy = code.stream().collect(Collectors.toList());
				thrown.add(new Pair<String, List<String>>(content.get(i), copy));
				i++;
				
				code.clear();
				
				String exc = null;
				
				while (i < content.size() && !content.get(i).equals("OUTPUT")) {
					if (content.get(i).equals("SOURCE") && !code.isEmpty() && exc != null) {
						List<String> cp = code.stream().collect(Collectors.toList());
						thrown.add(new Pair<String, List<String>>(exc, cp));
						code.clear();
						exc = null;
					}
					else if (content.get(i).equals("SOURCE")) {
						i++;
					}
					else {
						if (content.get(i).equals("THROWN")) {
							exc = content.get(i + 1);
							i += 3;
						}
						else {
							code.add(content.get(i));
							i++;
						}
					}
				}
				
				if (!code.isEmpty() && exc != null) {
					thrown.add(new Pair<String, List<String>>(exc, code));
				}
			}
			else {
				while (i < content.size() && !content.get(i).equals("OUTPUT")) {
					testcases.add(content.get(i));
					i++;
				}
			}
			
			for (int a = 0; a < content.size(); a++) {
				if (content.get(a).equals("OUTPUT")) {
					break;
				}
				else writeback.add(content.get(a));
			}
		
			buffer.add(new Message("Testing file " + file, LogPoint.Type.INFO, buffered));
			
			/* Run test */
			Result res = this.test(file, code, testcases, thrown, buffer, writeback);
			
			/* Evaluate result */
			if (res.fail > 0) resCnt.peek().failed++;
			else if (res.res == RET_TYPE.CRASH) resCnt.peek().crashed++;
			else if (res.res == RET_TYPE.TIMEOUT) resCnt.peek().timeout++;
			else buffer.add(new Message("Test finished successfully.", LogPoint.Type.INFO, true));
		
		} catch (Exception e) {
			e.printStackTrace();
			
			/* Test crashed */
			buffer.add(new Message("-> Test " + file + " ran into an error!", LogPoint.Type.FAIL, true));
			resCnt.peek().crashed++;
			e.printStackTrace();
		}
		
		return buffer;
	}
	
	@SuppressWarnings("deprecation")
	public Result test(String path, List<String> code, List<String> cases, List<Pair<String, List<String>>> thrown, List<Message> buffer, List<String> content) throws InterruptedException {
		CompilerDriver.reset();
		CompilerDriver cd = new CompilerDriver();
		CompilerDriver.driver = cd;
		
		int succ = 0;
		int fail = 0;
		
		if (thrown != null) {
			CompilerDriver.expectError = true;
			
			for (int i = 0; i < thrown.size(); i++) {
				cd.setBurstMode(!this.detailedCompilerMessages, this.displayCompilerImmediateRepresentations);
				
				/* Each exception test has own source code */
				cd.compile(new File(path), thrown.get(i).second);
				
				cd.setBurstMode(false, false);
				
				Exception e = cd.getException();
				
				if (e == null) {
					/* No exception was thrown, but an exception was expected */
					if (thrown.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + thrown.size() + " failed.", LogPoint.Type.FAIL, true));
					buffer.add(new Message("-> Expected Exception '" + thrown.get(i).first + "', but got none.", LogPoint.Type.FAIL, true));
					fail++;
				}
				else {
					String msg = null;
					
					/* Figure out exception field value */
					if (e instanceof SNIPS_EXC) 
						msg = ((SNIPS_EXC) e).getExcFieldName();
					else if (e instanceof CTX_EXC) 
						msg = ((CTX_EXC) e).getExcFieldName();
					else if (e instanceof PARSE_EXC) 
						msg = ((PARSE_EXC) e).getExcFieldName();
					else if (e instanceof CGEN_EXC) 
						msg = ((CGEN_EXC) e).getExcFieldName();
					else {
						System.out.println(new Message("Cannot get type of error " + e.getClass().getName(), LogPoint.Type.FAIL).getMessage());
						System.exit(0);
					}
					
					if (thrown.get(i).first.equals(msg)) {
						/* Thrown exception matches expected one */
						succ++;
					}
					else {
						/* Exceptions do not match */
						if (thrown.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + thrown.size() + " failed.", LogPoint.Type.FAIL, true));
						buffer.add(new Message("-> Thrown Exception: " + e, LogPoint.Type.FAIL, true));
						buffer.add(new Message("-> Thrown Exception does not match expected: " + msg + " vs " + thrown.get(i).first, LogPoint.Type.FAIL, true));
						fail++;
					}
				}
			}
		}
		else {
			cd.setBurstMode(!this.detailedCompilerMessages, this.displayCompilerImmediateRepresentations);
			
			File file = new File(path);
			List<String> compile = cd.compile(new File(path), code);

			List<String> copy = new ArrayList();
			for (String s : compile) copy.add("" + s);
			
			try {
				Linker.linkProgram(compile);
			} catch (LNK_EXC e) {
				cd.setBurstMode(false, false);
				buffer.add(new Message("Error when linking output!", LogPoint.Type.FAIL, true));
				return new Result(RET_TYPE.CRASH, 0, 0);
			}
			
			cd.setBurstMode(false, false);
			
			if (compile == null) {
				buffer.add(new Message("-> A crash occured during compilation.", LogPoint.Type.FAIL, true));
				if (this.printResult) buffer.add(new Message("-> Tested code:", LogPoint.Type.FAIL, true));
				cd.compile(file, code);
				return new Result(RET_TYPE.CRASH, 0, 0);
			}
			else {
				/* Write output */
				if (writebackResults) {
					content.add("OUTPUT");
					content.addAll(copy);
					Util.writeInFile(content, path);
				}
			}
			
			boolean printedOutput = this.printResult;
			
			if (this.printResult) {
				compile.stream().forEach(x -> System.out.println(x));
				printedOutput = true;
			}
			
			/* Setup Runtime Environment */
			for (int i = 0; i < cases.size(); i++) {
				String [] sp = cases.get(i).split(" ");
				
				boolean assemblyMessages = false;
				XMLNode head = new XMLParser(new File("res\\Test\\config.xml")).root;
				
				ProcessorUnit pcu0 = null;
				
				try {
					pcu0 = REv.Modules.Tools.Util.buildEnvironmentFromXML(head, compile, !assemblyMessages);
				} catch (Exception e) {
					buffer.add(new Message("Error generating assembly!", LogPoint.Type.FAIL, true));
					e.printStackTrace();
					
					buffer.add(new Message("-> Outputted Assemby Program: ", LogPoint.Type.FAIL, true));
					compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
					
					return new Result(RET_TYPE.CRASH, 0, 0);
				}
				
				ProcessorUnit pcu = pcu0;
				
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
				
				runThread.start();
				long start = System.currentTimeMillis();
				while (runThread.isAlive()) {
					if (System.currentTimeMillis() - start > ttl) {
						runThread.interrupt();
						runThread.stop();
						runThread = null;
						buffer.add(new Message("The compiled program timed out!", LogPoint.Type.FAIL, true));
						if (cases.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", LogPoint.Type.FAIL, true));
						fail++;
						if (!printedOutput) compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
						printedOutput = true;
						break;
					}
				}
				
				int pcu_return = REv.Modules.Tools.Util.toDecimal2K(pcu.regs [0]);
				this.totalCPUCycles += pcu.cycles;
				
				if (pcu_return == Integer.parseInt(sp [sp.length - 1])) {
					/* Output does match expected value */
					succ++;
				}
				else {
					/* Wrong output */
					if (cases.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", LogPoint.Type.FAIL, true));
					buffer.add(new Message("-> Expected <" + Integer.parseInt(sp [sp.length - 1]) + ">, actual <" + pcu_return + ">.", LogPoint.Type.FAIL, true));
					
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
					buffer.add(new Message(params, LogPoint.Type.FAIL, true));
					
					if (!printedOutput) {
						buffer.add(new Message("-> Outputted Assemby Program: ", LogPoint.Type.FAIL, true));
						compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
					}
					printedOutput = true;
					
					fail++;
				}
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
