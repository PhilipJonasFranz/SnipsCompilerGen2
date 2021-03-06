package Tst;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Exc.LINK_EXC;
import Exc.OPT0_EXC;
import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Lnk.Linker;
import Lnk.Linker.LinkerUnit;
import REv.CPU.ProcessorUnit;
import Res.Manager.FileUtil;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Util;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.Logging.SimpleMessage;
import XMLParser.MalformedXMLException;
import XMLParser.XMLParser;
import XMLParser.XMLParser.XMLNode;

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
	
	/** The amount of milliseconds the compiler can run for a given program before being considered timeouted. */
	public long MAX_COMPILE_TIME = 5000;
	
	/** Print the compiler messages for each test. */
	public boolean detailedCompilerMessages = false;
	
	/** Print the compiler immediate representations */
	public boolean displayCompilerImmediateRepresentations = false;
	
	/** If true, the metrics-inf.xml file will be updated. */
	public boolean updateNodeMetrics = false;
	
	/** Print the assembly compilation results */
	public boolean printResult = false;
	
	public boolean printResultOnError = true;
	
	/** Store/Update asm results in the tested file */
	public boolean writebackResults = false;
	
	public static boolean excludeASMErrors = false;
	
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
		CompilerDriver.buildModulesRecurse = true;
		CompilerDriver.buildObjectFileOnly = true;
		CompilerDriver.pruneModules = true;
		CompilerDriver.useExperimentalOptimizer = true;
		CompilerDriver.useDefaultVersionID = false;
		
		List<String> paths = new ArrayList();
		
		/* Add add files to the paths list */
		if (args.length == 0) paths.addAll(FileUtil.fileWalk("res\\Test\\").stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
		else {
			for (String s : args) {
				if (s.endsWith(".sn")) paths.add(s);
				else {
					paths.addAll(FileUtil.fileWalk(s).stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
				}
			}
		}
		
		amount = paths.size();
		
		//if (amount == 1) printResult = true;
		
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
		Util.printStats(CompilerDriver.driver);
		
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
			if (paths.size() > 1 && updateNodeMetrics) {
				new Message("Created new node metrics config.", LogPoint.Type.INFO);
				Util.flushAsNNodeMetrics();
			}
			
			new Message("[BUILD] Successful.", LogPoint.Type.INFO);
		}
		else new Message("[BUILD] Failed.", LogPoint.Type.FAIL);
		
		System.exit(0);
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
			List<String> content = FileUtil.readFile(new File(file));
		
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
				
				Exception e = cd.thrownException;
				
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
					else if (e instanceof CTEX_EXC) 
						msg = ((CTEX_EXC) e).getExcFieldName();
					else if (e instanceof PARS_EXC) 
						msg = ((PARS_EXC) e).getExcFieldName();
					else if (e instanceof CGEN_EXC) 
						msg = ((CGEN_EXC) e).getExcFieldName();
					else if (e instanceof OPT0_EXC) 
						msg = ((OPT0_EXC) e).getExcFieldName();
					else {
						CompilerDriver.outs.println(new Message("Cannot get type of error " + e.getClass().getName(), LogPoint.Type.FAIL).getMessage());
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
			
			List<String> compile = doCompile(cd, path, code);

			List<String> copy = new ArrayList();
			
			if (compile != null) {
				for (String s : compile) copy.add("" + s);
				
				try {
					LinkerUnit originUnit = Linker.parseLinkerUnit(compile);
					Linker.linkProgram(new ArrayList(), originUnit);
					compile = originUnit.build();
				} catch (LINK_EXC e) {
					cd.setBurstMode(false, false);
					buffer.add(new Message("Error when linking output! " + e.getMessage(), LogPoint.Type.FAIL, true));
					
					if (printResultOnError) compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
					
					return new Result(RET_TYPE.CRASH, 0, 0);
				}
			}
			
			cd.setBurstMode(false, false);
			
			if (compile == null) {
				if (timeout) {
					buffer.add(new Message("-> The compilation process timed out.", LogPoint.Type.FAIL, true));
					return new Result(RET_TYPE.TIMEOUT, 0, 0);
				}
				else {
					buffer.add(new Message("-> A crash occured during compilation.", LogPoint.Type.FAIL, true));
					if (this.printResult) buffer.add(new Message("-> Tested code:", LogPoint.Type.FAIL, true));
					doCompile(cd, path, code);
					return new Result(RET_TYPE.CRASH, 0, 0);
				}
			}
			else {
				/* Write output */
				if (writebackResults) {
					content.add("OUTPUT");
					content.addAll(copy);
					FileUtil.writeInFile(content, path);
				}
			}
			
			boolean printedOutput = this.printResult;
			
			if (this.printResult) {
				compile.stream().forEach(x -> CompilerDriver.outs.println(x));
				printedOutput = true;
			}
			
			/* Setup Runtime Environment */
			for (int i = 0; i < cases.size(); i++) {
				String [] sp = cases.get(i).split(" ");
				
				boolean assemblyMessages = false;
				XMLNode head = null;
				
				try {
					head = XMLParser.parse(new File("res\\Test\\config.xml"));
				} catch (MalformedXMLException e) {
					new Message("Failed to parse LLVM xml configuration!", LogPoint.Type.FAIL);
					System.exit(0);
				}
				
				ProcessorUnit pcu0 = null;
				
				boolean silenced = CompilerDriver.silenced;
				
				try {
					if (excludeASMErrors) CompilerDriver.silenced = true;
					
					pcu0 = REv.Modules.Tools.Util.buildEnvironmentFromXML(head, compile, !assemblyMessages);
					
					CompilerDriver.silenced = silenced;
				} catch (Exception e) {
					CompilerDriver.silenced = silenced;
					
					buffer.add(new Message("Error generating assembly!", LogPoint.Type.FAIL, true));
					
					if (!excludeASMErrors) {
						e.printStackTrace();
						
						if (printResultOnError) {
							buffer.add(new Message("-> Outputted Assemby Program: ", LogPoint.Type.FAIL, true));
							compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
						}
					}
					
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
					if (sp.length > 1) {
						String params = "-> Params: ";
						for (int a = 0; a < sp.length - 1; a++) {
							params += sp [a];
							if (a < sp.length - 2) {
								params += ", ";
							}
						}
						buffer.add(new Message(params, LogPoint.Type.FAIL, true));
					}
					
					if (printResultOnError && !printedOutput) {
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
	
	private boolean timeout = false;
	
	public List<String> doCompile(CompilerDriver cd, String path, List<String> code) {
		Object [] out = new Object [] {null};
		
		timeout = false;
		
		Thread compileThread = new Thread(new Runnable() {
			public void run() {
				out [0] = cd.compile(new File(path), code);
			}
		});
		
		compileThread.start();
		
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < MAX_COMPILE_TIME && compileThread.isAlive()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (cd.thrownException == null && System.currentTimeMillis() - start >= MAX_COMPILE_TIME) {
			timeout = true;
			return null;
		}
		
		CompilerDriver.reset();
		return (List<String>) out [0];
	}
	
}
