package Lnk;

import java.util.ArrayList;
import java.util.List;

import Exc.LINK_EXC;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Res.Manager.RessourceManager;
import Snips.CompilerDriver;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

/**
 * The linker is responsible for resolving '.include' directives
 * in the generated assembly to the actual assembly modules.
 * 
 * The modules are included and packed into a single translation
 * unit. The .text and .data Sections are merged. The output is
 * a list of strings that represent the linked executable.
 */
public class Linker {

			/* ---< NESTED >--- */
	/**
	 * Represents a single, asm-translated module. The module
	 * contains a list of imports, a data and text section, a
	 * version, and a source file. The version is read from the 
	 * source file from the .version directive.
	 */
	public static class LinkerUnit {
		
				/* ---< FIELDS >--- */
		/**
		 * Version ID of the module. Used to check if the module
		 * has changed and should be recompiled. The version number
		 * is determined by hashing the source code representation
		 * of the module.
		 */
		public long versionID = 0;
		
		/**
		 * The assembly source file this LinkerUnit was loaded and
		 * parsed from.
		 */
		public String sourceFile;
		
		/**
		 * A list of imports that this linker unit states via 
		 * .include directives.
		 */
		public List<String> imports = new ArrayList();
		
		/**
		 * The .data section if this unit.
		 */
		public List<String> dataSection = new ArrayList();
		
		/**
		 * The .text section of this unit.
		 */
		public List<String> textSection = new ArrayList();
		
		
				/* ---< METHODS >--- */
		/**
		 * Build out this unit to a List of strings. Inserts
		 * .data and .text annotations between the sections.
		 * Requires all imports to be resolved when the method
		 * is called.
		 * @return A list of strings representing the assembly of
		 * 		this linker unit.
		 */
		public List<String> build() {
			/* Make sure linker resolved all stated imports */
			assert imports.isEmpty() : "Attempted to build output with imports!";
			
			List<String> output = new ArrayList();
			
			output.add(".version " + this.versionID);
			output.add("");
			
			for (String imp : this.imports) 
				output.add(imp);
			if (!this.imports.isEmpty())
				output.add("");
			
			if (!this.dataSection.isEmpty()) {
				output.add(".data");
				for (String s : this.dataSection)
					if (!s.trim().equals(""))
						output.add(s);
			
				output.add("");
			}
			
			while (!this.textSection.isEmpty() && this.textSection.get(0).trim().equals("")) {
				this.textSection.remove(0);
			}
			
			if (!this.textSection.isEmpty() && !(this.textSection.size() == 1 && this.textSection.get(0).trim().equals(""))) {
				output.add(".text");
				
				for (int i = 0; i < this.textSection.size(); i++) {
					if (i == 0 && this.textSection.get(i).trim().equals("")) continue;
					output.add(this.textSection.get(i));
				}
			}
			
			for (int i = 1; i < output.size(); i++) {
				if (output.get(i).trim().isEmpty() && output.get(i - 1).trim().isEmpty()) {
					output.remove(i);
					i--;
				}
			}
			
			return output;
		}
		
		/**
		 * Prints out this linker unit to the Compiler output stream.
		 */
		public void print() {
			CompilerDriver.outs.println("LinkerUnit | Source File: " + ((this.sourceFile != null)? this.sourceFile : "Unknown"));
			this.build().stream().forEach(x -> CompilerDriver.outs.println(x));
			CompilerDriver.outs.println();
		}
		
	}
	
	/**
	 * Parse a LinkerUnit from the given assembly. The given assembly
	 * should be loaded from a .s module file.
	 * @param asm The assembly loaded from a .s file.
	 * @return The parsed linker unit.
	 */
	public static LinkerUnit parseLinkerUnit(List<String> asm) {
		LinkerUnit unit = new LinkerUnit();
		
		SECTION section = SECTION.TEXT;
		for (String line : asm) {
			if (line.startsWith(".version")) {
				unit.versionID = Long.parseLong(line.split(" ") [1]);
			}
			else if (line.startsWith(".data"))
				section = SECTION.DATA;
			else if (line.startsWith(".text"))
				section = SECTION.TEXT;
			else if (line.startsWith(".include"))
				unit.imports.add(line);
			else {
				if (section == SECTION.DATA)
					unit.dataSection.add(line);
				else if (section == SECTION.TEXT)
					unit.textSection.add(line);
			}
		}
		
		return unit;
	}
	
	public static ProgressMessage link_progress = null;
	
	public static List<Message> buffer = new ArrayList();
	
	/**
	 * For the given LinkerUnit, resolve all imports. Ignores duplicate and duplicate-transitive
	 * imports. In the included-list, all included imports are listed. Initialize this list to an empty list.
	 * The imports are added to the given linker unit.
	 * @param included All currently included imports. Initialize to empty list.
	 * @param unit The unit to link.
	 * @return All imports that were included within this method call. These are added to the included list.
	 * @throws LINK_EXC Thrown if an included module cannot be located.
	 */
	public static List<String> linkProgram(List<String> included, LinkerUnit unit) throws LINK_EXC {
		for (int i = unit.imports.size() - 1; i >= 0; i--) {
			String imp = unit.imports.get(i);
			
			String [] impSp = imp.trim().split(" ");
			
			boolean isMaybeInclude = false;
			
			String incPath = impSp [1];
			if (impSp [1].equals("maybe")) {
				incPath = impSp [2];
				isMaybeInclude = true;
			}
			
			String [] sp = incPath.split("@");
			
			String filePath = sp [0];
			String label = null;
			if (sp.length > 1) label = sp [1];
			
			String mappedPath = RessourceManager.instance.resolve(filePath);
			
			if (mappedPath.equals(unit.sourceFile)) {
				buffer.add(new Message("LINK -> Found self-import in file '" + unit.sourceFile + "'", Type.WARN));
				continue;
			}
			
			if (included.contains(incPath)) continue;
			else included.add(incPath);
			
			/* Recursive linking */
			List<String> lines = RessourceManager.instance.getFile(mappedPath);
			
			if (lines == null) {
				if (isMaybeInclude)
					continue;
				else
					throw new LINK_EXC("Failed to locate include target %s", mappedPath);
			}
			
			LinkerUnit importedUnit = Linker.parseLinkerUnit(lines);
			importedUnit.sourceFile = mappedPath;
			included = Linker.linkProgram(included, importedUnit);
			
			unit.dataSection.addAll(0, importedUnit.dataSection);
			
			if (label == null) {
				if (unit.textSection.size() > 4)
					unit.textSection.addAll(3, importedUnit.textSection);
				else
					unit.textSection.addAll(importedUnit.textSection);
				
				if (!importedUnit.textSection.isEmpty()) {
					if (unit.textSection.size() > 4)
						unit.textSection.add(3 + importedUnit.textSection.size(), "");
					else
						unit.textSection.add(importedUnit.textSection.size(), "");
				}
				
				buffer.add(new Message("LINK -> Resolved '" + incPath + "' to " + importedUnit.textSection.size() + " lines from '" + mappedPath + "'", Type.INFO, true));
			}
			else {
				boolean found = false;
				
				/* Search import */
				for (int a = 0; a < lines.size(); a++) {
					if (label == null || lines.get(a).trim().startsWith(".global " + label)) {
						/* Found position in module */
						found = true;
						
						int cnt = 3;
						
						while (true) {
							/* Copy contents until EOF is reached, or until a new .global directive is seen */
							if (a >= lines.size() || (cnt > 3 && lines.get(a).contains(".global"))) break;
							unit.textSection.add(cnt++, lines.get(a++));
						}
						
						buffer.add(new Message("LINK -> Resolved '" + incPath + "' to " + (cnt - 3) + " lines from '" + mappedPath + "'", Type.INFO, true));
						break;
					}
				}
				
				if (!found) 
					buffer.add(new Message("LINK -> Failed to locate '" + label + "' in '" + mappedPath + "'", Type.FAIL, true));
			}
		}
		
		unit.imports.clear();
		
		return included;
	}
	
}
