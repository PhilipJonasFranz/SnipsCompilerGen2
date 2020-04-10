package Tst;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import REv.CPU.ProcessorUnit;
import REv.Modules.Tools.XMLParser;
import REv.Modules.Tools.XMLParser.XMLNode;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Util;
import Util.Logging.Message;
import Util.Logging.SimpleMessage;

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
	
	public class ResultCnt {
		
		public int failed = 0, crashed = 0, timeout = 0;
		
		public ResultCnt() {
			
		}
		
		public ResultCnt(int f, int c, int t) {
			this.failed = f;
			this.crashed = c;
			this.timeout = t;
		}
		
		public void add(ResultCnt cnt) {
			this.failed += cnt.failed;
			this.crashed += cnt.crashed;
			this.timeout += cnt.timeout;
		}
		
	}
	
	public class TestNode {
		
		public TestNode parent;
		
		public String testPackage;
		
		public HashMap<String, TestNode> childPackages = new HashMap();
		
		public List<TestNode> childs = new ArrayList();
		
		public List<Pair<String, List<Message>>> tests = new ArrayList();
		
		public TestNode(List<String> files) {
			this.testPackage = "";
			
			List<String> cut = files.stream().map(x -> x.substring(testPackage.length())).collect(Collectors.toList());
			
			for (String file : cut) {
				this.addTest(file);
			}
		}
		
		public TestNode(TestNode parent, String testPackage) {
			this.parent = parent;
			this.testPackage = testPackage;
		}
		
		public String getPackagePath() {
			if (this.parent == null) return "";
			else return this.parent.getPackagePath() + this.testPackage + "/";
		}
		
		public void addTest(String file) {
			if (!file.contains("\\")) {
				this.tests.add(new Pair<String, List<Message>>(file, new ArrayList()));
			}
			else {
				String subPackage = "";
				while (!file.startsWith("\\")) {
					subPackage += "" + file.charAt(0);
					file = file.substring(1);
				}
				file = file.substring(1);
				
				if (!this.childPackages.containsKey(subPackage)) {
					TestNode node = new TestNode(this, subPackage);
					this.childPackages.put(subPackage, node);
					this.childs.add(node);
				}
				
				if (this.childPackages.containsKey(subPackage)) {
					this.childPackages.get(subPackage).addTest(file);
				}
			}
		}
		
		public void print(int d) {
			String s = "";
			for (int i = 0; i < d; i++) s += " ";
			System.out.println(s + this.testPackage);
			for (Pair<String, List<Message>> tests : this.tests) {
				System.out.println(s + "    " + "Test: " + tests.t0);
			}
			
			for (Entry<String, TestNode> entry : this.childPackages.entrySet()) {
				entry.getValue().print(d + 4);
			}
		}
		
	}
	
	/** Test result Status */
	public enum RET_TYPE {	
		SUCCESS, FAIL, CRASH, TIMEOUT
	}
	
	
			/* --- FIELDS --- */
	/** The amount of milliseconds the program can run on the processor until it counts as a timeout */
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
		CompilerDriver.silenced = false;
		
		List<String> paths = new ArrayList();
		
		if (args.length == 0) paths.addAll(this.getTestFiles("res\\Test\\").stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
		else {
			for (String s : args) {
				if (s.endsWith(".sn")) paths.add(s);
				else {
					paths.addAll(this.getTestFiles(s).stream().filter(x -> !x.startsWith("exclude_") && x.endsWith(".txt")).collect(Collectors.toList()));
				}
			}
		}
		
		if (paths.size() == 0) {
			new Message("Could not find any tests, make sure the path starts from the res/ folder.", Message.Type.WARN);
			new Message("Make sure the test files are .txt files.", Message.Type.WARN);
			System.exit(0);
		}
		
		new Message("Starting run, found " + paths.size() + " test" + ((paths.size() == 1)? "" : "s") + ".", Message.Type.INFO);
		
		TestNode head = new TestNode(paths);
		new Message("Successfully built package tree.", Message.Type.INFO);
		
		long start = System.currentTimeMillis();
		
		/* Base Layer */
		resCnt.push(new ResultCnt());
		
		testPackage(head);
		
		ResultCnt res = resCnt.pop();
		
		new Message("Finished " + paths.size() + " test" + ((paths.size() == 1)? "" : "s") + ((res.failed == 0 && res.crashed == 0 && res.timeout == 0)? " successfully in " + 
				(System.currentTimeMillis() - start) + " Millis" : ", " + res.failed + " test(s) failed" + 
				((res.crashed > 0)? ", " + res.crashed + " tests(s) crashed" : "")) + 
				((res.timeout> 0)? ", " + res.timeout + " tests(s) timed out" : "") + ".", 
				(res.failed == 0 && res.failed == 0)? Message.Type.INFO : Message.Type.FAIL);
		
		CompilerDriver.printAverageCompression();
		
		if (res.crashed == 0 && res.timeout == 0 && res.failed == 0) {
			new Message("[BUILD] Successful.", Message.Type.INFO);
		}
		else {
			new Message("[BUILD] Failed.", Message.Type.FAIL);
		}
	}
	
	public void testPackage(TestNode node) {
		boolean buffered = !(detailedCompilerMessages || displayCompilerImmediateRepresentations || printResult);
		
		Message headMessage = new Message("Testing Package " + node.getPackagePath(), Message.Type.INFO, buffered);
		boolean printedHead = false;
		
		/* Push summary for package */
		resCnt.push(new ResultCnt());
		
		if (!node.childs.isEmpty()) {
			for (TestNode node0 : node.childs) {
				testPackage(node0);
			}
		}
		
		for (Pair<String, List<Message>> test : node.tests) {
			resCnt.push(new ResultCnt());
			
			test.t1.addAll(runTest(node.getPackagePath() + test.t0));
			
			ResultCnt res = resCnt.pop();
			
			if (res.timeout > 0 || res.crashed > 0 || res.failed > 0) {
				if (!printedHead) {
					headMessage.flush();
					printedHead = true;
				}
				test.t1.stream().forEach(x -> x.flush());
			}
			
			resCnt.peek().add(res);
		}
		
		ResultCnt res = resCnt.pop();
		
		if ((res.timeout > 0 || res.crashed > 0 || res.failed > 0) && !node.tests.isEmpty()) {
			new Message("Package Tests failed: " + node.getPackagePath(), Message.Type.FAIL);
		}
		
		resCnt.peek().add(res);
	}
	
	Stack<ResultCnt> resCnt = new Stack();
	
	public List<Message> runTest(String file) {
		boolean buffered = !(detailedCompilerMessages || displayCompilerImmediateRepresentations || printResult);
		
		List<Message> buffer = new ArrayList();
		try {
			/* Read content of test file */
			List<String> content = Util.readFile(new File(file));
		
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
		
			buffer.add(new Message("Testing file " + file, Message.Type.INFO, buffered));
			
			/* Run test */
			Result res = this.test(file, code, testcases, buffer);
			
			if (res.fail > 0) resCnt.peek().failed++;
			else if (res.res == RET_TYPE.CRASH) resCnt.peek().crashed++;
			else if (res.res == RET_TYPE.TIMEOUT) resCnt.peek().timeout++;
			else buffer.add(new Message("Test finished successfully.", Message.Type.INFO, true));
		
		} catch (Exception e) {
			buffer.add(new Message("-> Test " + file + " ran into an error!", Message.Type.FAIL, true));
			resCnt.peek().crashed++;
			e.printStackTrace();
		}
		
		return buffer;
	}
	
	@SuppressWarnings("deprecation")
	public Result test(String path, List<String> code, List<String> cases, List<Message> buffer) throws InterruptedException {
		CompilerDriver cd = new CompilerDriver();
		CompilerDriver.driver = cd;
		
		cd.setBurstMode(!this.detailedCompilerMessages, this.displayCompilerImmediateRepresentations);
		
		File file = new File(path);
		List<String> compile = cd.compile(new File(path), code);
		
		cd.setBurstMode(false, false);
		
		if (compile == null) {
			buffer.add(new Message("-> A crash occured during compilation.", Message.Type.FAIL, true));
			if (this.printResult) buffer.add(new Message("-> Tested code:", Message.Type.FAIL, true));
			cd.compile(file, code);
			return new Result(RET_TYPE.CRASH, 0, 0);
		}
		
		boolean printedOutput = this.printResult;
		
		if (this.printResult) {
			compile.stream().forEach(x -> System.out.println(x));
			printedOutput = true;
		}
		
		int succ = 0;
		int fail = 0;
		
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
					buffer.add(new Message("The compiled program timed out!", Message.Type.FAIL, true));
					if (cases.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", Message.Type.FAIL, true));
					fail++;
					if (!printedOutput) compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
					printedOutput = true;
				}
			}
			
			int pcu_return = REv.Modules.Tools.Util.toDecimal2K(pcu.regs [0]);
			
			if (pcu_return == Integer.parseInt(sp [sp.length - 1])) {
				/* Output does match expected value */
				succ++;
			}
			else {
				/* Wrong output */
				if (cases.size() > 1) buffer.add(new Message("Testcase " + (i + 1) + "/" + cases.size() + " failed.", Message.Type.FAIL, true));
				buffer.add(new Message("-> Expected <" + Integer.parseInt(sp [sp.length - 1]) + ">, actual <" + pcu_return + ">.", Message.Type.FAIL, true));
				
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
				buffer.add(new Message(params, Message.Type.FAIL, true));
				
				if (!printedOutput) {
					buffer.add(new Message("-> Outputted Assemby Program: ", Message.Type.FAIL, true));
					compile.stream().forEach(x -> buffer.add(new SimpleMessage(CompilerDriver.printDepth + x, true)));
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
