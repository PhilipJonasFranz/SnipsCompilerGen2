package Lnk;

import java.util.ArrayList;
import java.util.List;

import Exc.LNK_EXC;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import PreP.PreProcessor;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

public class Linker {

	public static class LinkerUnit {
		
		public String sourceFile;
		
		public List<String> imports = new ArrayList();
		
		public List<String> dataSection = new ArrayList();
		
		public List<String> textSection = new ArrayList();
		
		public List<String> build() {
			assert imports.isEmpty() : "Attempted to build output with imports!";
			
			List<String> output = new ArrayList();
			
			output.add(".data");
			for (String s : this.dataSection)
				if (!s.trim().equals(""))
					output.add(s);
			
			if (!this.dataSection.isEmpty())
				output.add("");
			
			output.add(".text");
			
			for (int i = 0; i < this.textSection.size(); i++) {
				if (i == 0 && this.textSection.get(i).trim().equals("")) continue;
				output.add(this.textSection.get(i));
			}
			
			for (int i = 1; i < output.size(); i++) {
				if (output.get(i).trim().isEmpty() && output.get(i - 1).trim().isEmpty()) {
					output.remove(i);
					i--;
				}
			}
			
			return output;
		}
		
		public void print() {
			System.out.println("LinkerUnit | Source File: " + ((this.sourceFile != null)? this.sourceFile : "Unknown"));
			this.build().stream().forEach(x -> System.out.println(x));
			System.out.println();
		}
		
	}
	
	public static LinkerUnit parseLinkerUnit(List<String> asm) {
		LinkerUnit unit = new LinkerUnit();
		
		SECTION section = SECTION.TEXT;
		for (String line : asm) {
			if (line.startsWith(".data"))
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
	
	public static ProgressMessage link_progress = new ProgressMessage("LINK -> Starting", 30, LogPoint.Type.INFO);
	
	public static List<Message> buffer = new ArrayList();
	
	public static List<String> linkProgram(LinkerUnit unit) throws LNK_EXC {
		List<String> included = new ArrayList();
		
		for (String imp : unit.imports) {
			String incPath = imp.trim().split(" ") [1];
			String [] sp = incPath.split("@");
			
			String filePath = sp [0];
			String label = null;
			if (sp.length > 1) label = sp [1];
			
			String mappedPath = PreProcessor.resolveToPath(filePath);
			
			if (included.contains(incPath)) continue;
			else included.add(incPath);
			
			/* Recursive linking */
			List<String> lines = PreProcessor.getFile(mappedPath);
			
			if (lines == null) {
				throw new LNK_EXC("Failed to locate include target %s", mappedPath);
			}
			
			LinkerUnit importedUnit = Linker.parseLinkerUnit(lines);
			importedUnit.sourceFile = mappedPath;
			included.addAll(Linker.linkProgram(importedUnit));
			
			unit.dataSection.addAll(0, importedUnit.dataSection);
			
			if (label == null) {
				unit.textSection.addAll(3, importedUnit.textSection);
				if (!importedUnit.textSection.isEmpty()) unit.textSection.add(3 + importedUnit.textSection.size(), "");
				buffer.add(new Message("Resolved '" + mappedPath + "' to " + importedUnit.textSection.size() + " lines from '" + mappedPath + "'", Type.INFO, true));
			}
			else {
				boolean found = false;
				
				// Search import
				for (int a = 0; a < lines.size(); a++) {
					if (label == null || lines.get(a).trim().startsWith(".global " + label)) {
						// Found position in module
						found = true;
						
						int cnt = 3;
						
						while (true) {
							// Copy contents until EOF is reached, or until a new .global directive is seen
							if (a >= lines.size() || (cnt > 3 && lines.get(a).contains(".global"))) break;
							
							unit.textSection.add(cnt++, lines.get(a++));
						}
						
						buffer.add(new Message("Resolved '" + label + "' to " + (cnt - 3) + " lines from '" + mappedPath + "'", Type.INFO, true));
						break;
					}
				}
				
				if (!found) 
					buffer.add(new Message("Failed to locate '" + label + "' in '" + mappedPath + "'", Type.FAIL, true));
			}
		}
		
		return included;
	}
	
}
