package PreP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Exc.SNIPS_EXC;
import Res.Const;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Source;
import Util.Util;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import XMLParser.XMLParser.XMLNode;

public class PreProcessor {

	public static List<String> passedFlags = new ArrayList();
	
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
	
	public static List<String> imports = new ArrayList();
	
	public static int modulesIncluded = 0;
	
	/**
	 * Create new instance, convert code input into LineObject representation
	 */
	public PreProcessor(List<String> code, String filePath) {
		for (int i = 0; i < code.size(); i++) 
			this.process.add(new LineObject(i + 1, code.get(i), filePath));
	}
	
	public List<LineObject> getProcessed() {
		this.getProcessedR(new ArrayList());
		
		if (CompilerDriver.printAllImports)
			while (!imports.isEmpty())
				new Message("PRE0 -> Imported library " + imports.remove(0), LogPoint.Type.INFO);
		
		for (int i = 0; i < this.process.size(); i++) 
			this.process.get(i).line = this.process.get(i).line.replaceAll("__EN_SID", "" + !CompilerDriver.disableStructSIDHeaders);
		
		return this.process;
	}
	
	/**
	 * Convert the input to line objects and resolve #include directives, while maintaining correct file markers.
	 */
	public List<LineObject> getProcessedR(List<Pair<String, String>> passedFlags) {
		for (int i = 0; i < this.process.size(); i++) {
			String line = this.process.get(i).line.trim();
			if (line.startsWith("#ifdef")) {
				String s = this.process.remove(i).line.trim();
				String [] sp = s.split(" ");
				
				String cond = line.substring(sp [0].length() + 1);
				boolean eval = PreProcessor.evaluate(cond);
				
				int c = 1;
				if (eval) {
					int a = i;
					while (a != this.process.size()) {
						String s0 = this.process.get(a).line.trim();
						
						if (s0.startsWith("#end")) c--;
						else if (s0.startsWith("#ifdef")) c++;
						
						if (c == 0) {
							this.process.remove(a);
							break;
						}
						
						a++;
					}
				}
				else {
					while (i != this.process.size()) {
						String s0 = this.process.get(i).line.trim();
						
						if (s0.startsWith("#end")) c--;
						else if (s0.startsWith("#ifdef")) c++;
						
						this.process.remove(i);
						if (c == 0) break;
					}
				}
			}
			else if (line.startsWith("#define")) {
				String s = this.process.remove(i).line.trim();
				String [] sp = s.split(" ");
				
				/* Overwrite #define replacements with value passed in flag */
				for (Pair<String, String> flag : passedFlags) {
					if (flag.first.equals(sp [1]))
						sp [2] = flag.second;
				}
				
				for (int a = i; a < this.process.size(); a++) {
					String s0 = this.process.get(a).line;
					if (s0.contains(sp [1])) s0 = s0.replace(sp [1], sp [2]);
					this.process.get(a).line = s0;
				}
				
				i--;
			}
			else if (line.startsWith("#include")) {
				String s = line.substring(8).trim();
				
				if (s.startsWith("<") && s.endsWith(">")) {
					String path = s.substring(1, s.length() - 1);
					
					List<Pair<String, String>> passedFlags0 = new ArrayList();
					passedFlags0.addAll(passedFlags);
					
					/* Contains flag args */
					if (path.contains("<")) {
						String [] sp = path.split(">");
						path = sp [0];
						String args = sp [1].substring(2);
						
						sp = args.split(",");
						for (String s0 : sp) {
							s0 = s0.trim();
							String [] sp1 = s0.split("=");
							passedFlags0.add(new Pair<String, String>(sp1 [0].trim(), sp1 [1].trim()));
						}
					}
					
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
					 * Loading module from .sn file, check if a header exists, 
					 * and if yes, load it. This is nessesary since the header may
					 * include struct typedefs etc.
					 */
					if (path.endsWith(".sn")) {
						String modPath = path.substring(0, path.length() - 2) + "hn";
						if (getFile(modPath) != null) 
							i = this.includeLines(modPath, i, passedFlags0);
					}
					
					/* Load originally included file */
					i = this.includeLines(path, i, passedFlags0);
					
					/* 
					 * Recompiling all modules and we include header, this means we also have
					 * to load in the .sn counterpart.
					 */
					if (CompilerDriver.buildModulesRecurse && path.endsWith(".hn")) {
						String modPath = path.substring(0, path.length() - 2) + "sn";
						if (getFile(modPath) != null) 
							i = this.includeLines(modPath, i, passedFlags0);
					}
					
					i--;
				} 
				else {
					new Message("PRE0 -> Found line: " + line + ", but cannot resolve! Ensure correct syntax, " + new Source(this.process.get(i).fileName, this.process.get(i).lineNumber, 0).getSourceMarker(), LogPoint.Type.WARN);
					new Message("PRE0 -> Import Manager may be able to resolve import. Verify output.", LogPoint.Type.WARN);
				}
			}
		}
		
		return this.process;
	}
	
	public int includeLines(String path, int i, List<Pair<String, String>> passedFlags) {
		/* 
		 * Build the include path that reflects the imported file and 
		 * the passed flags from the include directive.
		 */
		String incPath = path;
		if (!passedFlags.isEmpty()) {
			incPath += " | ";
			for (Pair<String, String> flag : passedFlags) {
				incPath += flag.first + "=" + flag.second + ", ";
			}
			incPath = incPath.substring(0, incPath.length() - 2);
		}
		
		if (!imports.contains(incPath)) {
			try {
				imports.add(incPath);
				modulesIncluded++;
				
				List<String> code = getFile(path);
				
				PreProcessor processor = new PreProcessor(code, path);
				List<LineObject> lines = processor.getProcessedR(passedFlags);
				
				this.process.addAll(i, lines);
				i += lines.size();
				
				if (CompilerDriver.buildModulesRecurse)
					new Message("Recompiling module: " + path, Type.INFO);
			} catch (NullPointerException e) {
				throw new SNIPS_EXC(Const.CANNOT_RESOLVE_IMPORT, path, new Source(this.process.get(i).fileName, this.process.get(i).lineNumber, 0).getSourceMarker());
			}
		}
		
		return i;
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
	
	public static boolean evaluate(String cond) {
		cond = cond.trim();
		if (cond.equals("true")) return true;
		else if (cond.equals("false")) return false;
		else if (passedFlags.contains(cond)) return true;
		else {
			/* TODO: Implement ||, &&, ! */
			return false;
		}
	}
	
} 
