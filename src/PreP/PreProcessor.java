package PreP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Snips.CompilerDriver;
import Util.Util;
import Util.XMLParser.XMLNode;
import Util.Logging.Message;
import lombok.AllArgsConstructor;

public class PreProcessor {

	@AllArgsConstructor
	public class LineObject {
		
		public int lineNumber;
		
		public String line;
		
		public String fileName;
	}
	
	List<LineObject> process = new ArrayList();
	
	public List<String> imported = new ArrayList();
	
	/**
	 * Create new instance, convert code input into LineObject representation
	 */
	public PreProcessor(List<String> code, String filePath) {
		for (int i = 0; i < code.size(); i++) {
			this.process.add(new LineObject(i + 1, code.get(i), filePath));
		}
	}
	
	/**
	 * Convert the input to line objects and resolve #include directives, while maintaining correct file markers.
	 */
	public List<LineObject> getProcessed() {
		for (int i = 0; i < this.process.size(); i++) {
			String line = this.process.get(i).line.trim();
			if (line.startsWith("#include")) {
				String s = line.substring(8).trim();
				
				if (s.startsWith("<") && s.endsWith(">")) {
					String path = s.substring(1, s.length() - 1);
					
					this.process.remove(i);
					
					if (!this.imported.contains(path)) {
						new Message("SNIPS_PREP -> Resolved import " + path, Message.Type.INFO);
						this.imported.add(path);
						List<String> lines = getFile(path);
						for (int a = 0; a < lines.size(); a++) this.process.add(i + a, new LineObject(a + 1, lines.get(a), path));
					}
					
					i--;
				} 
				else {
					new Message("SNIPS_PREP -> Found line: " + line + ", but cannot resolve! Ensure correct syntax.", Message.Type.WARN);
					new Message("SNIPS_PREP -> Import Manager may be able to resolve import. Verify output.", Message.Type.WARN);
				}
			}
		}
		
		return this.process;
	}
	
	public List<String> getFile(String filePath) {
		for (XMLNode c : CompilerDriver.sys_config.getNode("Library").children) {
			String [] v = c.value.split(":");
			if (v [0].equals(filePath)) {
				filePath = v [1];
			}
		}
		
		File file = new File(filePath);
		
		/* Read from file */
		List<String> code = Util.readFile(file);
		
		/* Read from jar */
		if (code == null) {
			code = CompilerDriver.driver.readFromJar(filePath);
		}
		
		return code;
	}
	
}
