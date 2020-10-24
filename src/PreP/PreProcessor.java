package PreP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Exc.SNIPS_EXC;
import Res.Const;
import Snips.CompilerDriver;
import Util.Source;
import Util.Util;
import Util.XMLParser.XMLNode;
import Util.Logging.LogPoint;
import Util.Logging.Message;

public class PreProcessor {

	public class LineObject {
		
				/* ---< FIELDS >--- */
		public int lineNumber;
		
		public String line;
		
		public String fileName;
		
		
				/* ---< CONSTRUCTORS >--- */
		public LineObject(int lineNumber, String line, String fileName) {
			this.lineNumber = lineNumber;
			this.line = line;
			this.fileName = fileName;
		}
		
	}
	
	List<LineObject> process = new ArrayList();
	
	public List<String> imported = new ArrayList();
	
	/**
	 * Create new instance, convert code input into LineObject representation
	 */
	public PreProcessor(List<String> code, String filePath) {
		for (int i = 0; i < code.size(); i++) 
			this.process.add(new LineObject(i + 1, code.get(i), filePath));
	}
	
	/**
	 * Convert the input to line objects and resolve #include directives, while maintaining correct file markers.
	 */
	public List<LineObject> getProcessed() {
		Stack<String> imports = new Stack();
		
		for (int i = 0; i < this.process.size(); i++) {
			String line = this.process.get(i).line.trim();
			if (line.startsWith("#include")) {
				String s = line.substring(8).trim();
				
				if (s.startsWith("<") && s.endsWith(">")) {
					String path = s.substring(1, s.length() - 1);
					
					this.process.remove(i);
					
					if (!this.imported.contains(path)) {
						try {
							List<String> lines = getFile(path);
							for (int a = 0; a < lines.size(); a++) 
								this.process.add(i + a, new LineObject(a + 1, lines.get(a), path));
							
							imports.push(path);
							
							this.imported.add(path);
						} catch (NullPointerException e) {
							throw new SNIPS_EXC(Const.CANNOT_RESOLVE_IMPORT, path, new Source(this.process.get(i).fileName, this.process.get(i).lineNumber, 0).getSourceMarker());
						}
					}
					
					i--;
				} 
				else {
					new Message("PRE0 -> Found line: " + line + ", but cannot resolve! Ensure correct syntax, " + new Source(this.process.get(i).fileName, this.process.get(i).lineNumber, 0).getSourceMarker(), LogPoint.Type.WARN);
					new Message("PRE0 -> Import Manager may be able to resolve import. Verify output.", LogPoint.Type.WARN);
				}
			}
		}
		
		if (CompilerDriver.printAllImports)
			while (!imports.isEmpty())
				new Message("PRE0 -> Imported library " + imports.pop(), LogPoint.Type.INFO);
		
		for (int i = 0; i < this.process.size(); i++) 
			this.process.get(i).line = this.process.get(i).line.replaceAll("__EN_SID", "" + !CompilerDriver.disableStructSIDHeaders);
		
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
		
		/* Read from release library */
		if (code == null) {
			file = new File("release\\" + filePath);
			code = Util.readFile(file);
		}
		
		/* Use path relative to input file */
		if (code == null) {
			file = new File(CompilerDriver.inputFile.getParent() + "\\" + filePath);
			code = Util.readFile(file);
		}
		
		return code;
	}
	
} 
