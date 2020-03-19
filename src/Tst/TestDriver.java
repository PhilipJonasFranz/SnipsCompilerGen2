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

	/** The Milliseconds the program can run on the processor until it timeouts */
	public long ttl = 200;
	
	/** Test case statuses */
	public enum RET_TYPE {	
		SUCCESS, FAIL, CRASH, TIMEOUT
	}
	
	public static void main(String [] args) {
		new TestDriver(args);	
	}
	
	public TestDriver(String [] args) {
		/* Setup Compiler Driver */
		CompilerDriver.printLogo();
		CompilerDriver.useTerminalColors = true;
		
		List<String> paths = new ArrayList();
		
		if (args.length == 0) paths.addAll(this.getTestFiles().stream().filter(x -> x.endsWith(".txt")).collect(Collectors.toList()));
		else paths.add(args [0]);
		
		long start = System.currentTimeMillis();
		int failed = 0, crashed = 0, timeout = 0;
		
		System.out.println(new Message("Starting Run, found " + paths.size() + " tests.", Message.Type.INFO).getMessage());
		
		String current = null;
		for (String file : paths) {
			try {
				/* Read content of test file */
				List<String> content = Util.readFile(new File(file));
				current = file;
			
				/* Extract contents */
				List<String> code = new ArrayList();
				List<String> params = new ArrayList();
				int returnValue = 0;
				
				int i = 1;
				while (true) {
					if (content.get(i).equals("PARAMS")) {
						i++;
						break;
					}
					code.add(content.get(i));
					i++;
				}
				while (true) {
					if (content.get(i).equals("VALIDATION")) {
						i++;
						break;
					}
					params.add(content.get(i));
					i++;
				}
				returnValue = Integer.parseInt(content.get(i));
			
				System.out.println(new Message("Testing file " + file, Message.Type.INFO).getMessage());
				
				/* Run test */
				RET_TYPE ret = this.test(file, code, params, returnValue);
				
				if (ret == RET_TYPE.FAIL) {
					failed++;
				}
				else if (ret == RET_TYPE.CRASH) {
					crashed++;
				}
				else if (ret == RET_TYPE.TIMEOUT) {
					timeout++;
				}
				else System.out.println(new Message("Test finished successfully.", Message.Type.INFO).getMessage());
				
			} catch (Exception e) {
				System.out.println(new Message("-> Test " + current + " ran into an error!", Message.Type.FAIL).getMessage());
				crashed++;
				e.printStackTrace();
			}
		}
		
		System.out.println(new Message("Finished " + paths.size() + " tests" + ((failed == 0 && crashed == 0 && timeout == 0)? " successfully in " + 
				(System.currentTimeMillis() - start) + " Millis." : ", " + failed + " test(s) failed" + 
				((crashed > 0)? ", " + crashed + " tests(s) crashed" : ((timeout > 0)? ", " : ".")) + 
				((timeout > 0)? ", " + timeout + " tests(s) timed out." : ".")), 
				(failed == 0 && crashed == 0)? Message.Type.INFO : Message.Type.FAIL).getMessage());
	}
	
	@SuppressWarnings("deprecation")
	public RET_TYPE test(String path, List<String> code, List<String> params, int validation) throws InterruptedException {
		File f = new File(path);
		CompilerDriver cd = new CompilerDriver(f, code);
		
		CompilerDriver.silenced = true;
		CompilerDriver.imm = false;
		
		List<String> compile = cd.compile();
		Thread.sleep(10);
		
		if (compile == null) {
			System.out.println(new Message("-> A crash occured during compilation.", Message.Type.FAIL).getMessage());
			System.out.println(new Message("-> Tested code:", Message.Type.FAIL).getMessage());
			CompilerDriver.silenced = false;
			CompilerDriver.imm = true;
			cd.compile();
			return RET_TYPE.CRASH;
		}
		
		/* Setup Runtime Environment */
		XMLNode head = new XMLParser(new File("res\\Test\\config.xml")).root;
		ProcessorUnit pcu = REv.Modules.Tools.Util.buildEnvironmentFromXML(head, compile);
		
		for (int i = 0; i < params.size(); i++) {
			pcu.regs [i] = pcu.toBinary(Integer.parseInt(params.get(i)));
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
				System.out.println(new Message("The compiled program timed out!", Message.Type.FAIL).getMessage());
				compile.stream().forEach(x -> System.out.println(x));
				return RET_TYPE.TIMEOUT;
			}
		}
		
		int pcu_return = REv.Modules.Tools.Util.toDecimal2K(pcu.regs [0]);
		
		if (pcu_return == validation) {
			/* Output does match expected value */
			return RET_TYPE.SUCCESS;
		}
		else {
			/* Wrong output */
			System.out.println(new Message("-> Expected <" + validation + ">, actual <" + pcu_return + ">.", Message.Type.FAIL).getMessage());
			System.out.println(new Message("-> Outputted Assemby Program: ", Message.Type.FAIL).getMessage());
			compile.stream().forEach(x -> System.out.println(x));
			return RET_TYPE.FAIL;
		}
	}
	
	/**
	 * Recursiveley get all test files from the res/Test/ directory
	 * @return A list of all file names.
	 */
	public List<String> getTestFiles() {
		try (Stream<Path> walk = Files.walk(Paths.get("res\\Test\\"))) {
			List<String> result = walk.filter(Files::isRegularFile)
					.map(x -> x.toString()).collect(Collectors.toList());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
