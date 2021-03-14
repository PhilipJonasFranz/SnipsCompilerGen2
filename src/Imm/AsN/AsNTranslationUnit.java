package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Directive.ASMDirective;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;

public class AsNTranslationUnit extends AsNNode {

	public String sourceFile;
	
	public List<String> imports = new ArrayList();
	
	public List<ASMInstruction> dataSection = new ArrayList();
	
	public List<ASMInstruction> textSection = new ArrayList();
	
	public AsNTranslationUnit(String sourceFile) {
		this.sourceFile = sourceFile;
	}
	
	/**
	 * Appends the given instruction to the given section.
	 */
	public void append(ASMInstruction instruction, SECTION section) {
		List<ASMInstruction> instructions = new ArrayList();
		instructions.add(instruction);
		this.append(instructions, section);
	}
	
	/**
	 * Appends the given instruction list to the given section.
	 */
	public void append(List<ASMInstruction> instructions, SECTION section) {
		if (section == SECTION.TEXT) {
			if (!textSection.isEmpty() && !(textSection.get(textSection.size() - 1) instanceof ASMSeperator))
				textSection.add(new ASMSeperator());
			
			this.textSection.addAll(instructions);
		}
		else if (section == SECTION.DATA) {
			this.dataSection.addAll(0, instructions);
			dataSection.add(instructions.size(), new ASMSeperator());
		}
	}
	
	/**
	 * Builds this translation unit. This means combining all sections, 
	 * and adding section headers between them.
	 * @return A list of all combined asm instructions.
	 */
	public List<ASMInstruction> buildTranslationUnit() {
		List<ASMInstruction> out = new ArrayList();
		
		/* Add imports */
		if (!this.imports.isEmpty()) {
			for (String i : this.imports) 
				out.add(new ASMDirective(".include " + i));
			
			out.add(new ASMSeperator());
		}
		
		/* Add data section */
		if (!this.dataSection.isEmpty()) {
			ASMSectionAnnotation data = new ASMSectionAnnotation(SECTION.DATA);
			out.add(data);
			
			out.addAll(this.dataSection);
			
			out.add(new ASMSeperator());
		}
		
		/* Add text section */
		if (!this.textSection.isEmpty()) {
			ASMSectionAnnotation text = new ASMSectionAnnotation(SECTION.TEXT);
			out.add(text);
			
			out.addAll(this.textSection);
		}
		
		return out;
	}
	
} 
