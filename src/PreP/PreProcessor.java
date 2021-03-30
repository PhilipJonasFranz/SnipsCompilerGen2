package PreP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import Exc.SNIPS_EXC;
import Res.Const;
import Snips.CompilerDriver;
import Util.Source;
import Util.Util;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import XMLParser.XMLParser.XMLNode;

public class PreProcessor {

	public static HashMap<String, List<String>> importsPerFile = new HashMap();
	
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
	
	public static int modulesIncluded = 0;
	
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
					
					/* Keep track which imports were made by which file */
					String asmPath = Util.toASMPath(this.process.get(i).fileName);
					List<String> importsFromFile = new ArrayList();
					if (importsPerFile.containsKey(asmPath))
						importsFromFile = importsPerFile.get(asmPath);
					else
						importsPerFile.put(asmPath, importsFromFile);
					
					String imp = Util.toASMPath(path);
					if (!importsFromFile.contains(imp))
						importsFromFile.add(imp);
					
					/* Remove import from source, replace with imported lines */
					this.process.remove(i);
					
					/* 
					 * Recompiling all modules and we include header, this means we also have
					 * to load in the .sn counterpart.
					 */
					if (CompilerDriver.buildModulesRecurse && path.endsWith(".hn")) {
						String modPath = path.substring(0, path.length() - 2) + "sn";
						if (getFile(modPath) != null) 
							this.includeLines(modPath, imports, i);
					}
					
					/* Load originally included file */
					this.includeLines(path, imports, i);
					
					/* 
					 * Loading module from .sn file, check if a header exists, 
					 * and if yes, load it. This is nessesary since the header may
					 * include struct typedefs etc.
					 */
					if (path.endsWith(".sn")) {
						String modPath = path.substring(0, path.length() - 2) + "hn";
						if (getFile(modPath) != null) 
							this.includeLines(modPath, imports, i);
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
	
	public void includeLines(String path, Stack<String> imports, int i) {
		if (!this.imported.contains(path)) {
			try {
				List<String> lines = getFile(path);
				for (int a = 0; a < lines.size(); a++) 
					this.process.add(i + a, new LineObject(a + 1, lines.get(a), path));
				
				imports.push(path);
				
				this.imported.add(path);
				
				modulesIncluded++;
				if (CompilerDriver.buildModulesRecurse)
					new Message("Recompiling module: " + path, Type.INFO);
			} catch (NullPointerException e) {
				throw new SNIPS_EXC(Const.CANNOT_RESOLVE_IMPORT, path, new Source(this.process.get(i).fileName, this.process.get(i).lineNumber, 0).getSourceMarker());
			}
		}
	}
	
	public static List<String> getFile(String filePath) {
		for (XMLNode c : CompilerDriver.sys_config.getNode("Library").getChildren()) {
			String [] v = c.getValue().split(":");
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
	
	public static String resolveToPath(String filePath) {
		for (XMLNode c : CompilerDriver.sys_config.getNode("Library").getChildren()) {
			String [] v = c.getValue().split(":");
			
			String modPath = v [0];
			if (modPath.equals(filePath)) {
				return "release/" + v [1];
			}
			
			modPath = Util.toASMPath(v [0]);
			if (modPath.equals(filePath)) {
				return "release/" + Util.toASMPath(v [1]);
			}
		}
		
		File file = new File(filePath);
		
		/* Read from file */
		List<String> code = Util.readFile(file);
		
		if (code != null) return filePath;
		else {
			file = new File("release/" + filePath);
			code = Util.readFile(file);
		}
		
		if (code != null) {
			return "release/" + filePath;
		}
		else {
			filePath = filePath.replace("\\", "/");
			String [] sp = filePath.split("/") ;
			String in = CompilerDriver.inputFile.getParent();
			return in + "/" + sp [sp.length - 1];
		}
	}
	
} 
