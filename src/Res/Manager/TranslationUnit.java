package Res.Manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Directive.ASMDirective;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.AsN.AsNNode;
import Lnk.Linker;
import Lnk.Linker.LinkerUnit;
import Snips.CompilerDriver;

public class TranslationUnit extends AsNNode {

	public LinkerUnit existingUnit;
	
	public long versionID = 0;
	
	public String sourceFile;
	
	public List<String> imports = new ArrayList();
	
	public List<ASMInstruction> dataSection = new ArrayList();
	
	public List<ASMInstruction> textSection = new ArrayList();
	
	public TranslationUnit(String sourceFile) {
		this.sourceFile = sourceFile;
		
		String mappedPath = RessourceManager.instance.resolve(sourceFile);
		List<String> existingModule = FileUtil.readFile(new File(mappedPath));
		if (existingModule != null) 
			this.existingUnit = Linker.parseLinkerUnit(existingModule);
	}
	
	public boolean hasVersionChanged() {
		return existingUnit == null || (existingUnit != null && existingUnit.versionID != this.versionID);
	}
	
	/**
	 * Appends the given instruction to the given section.
	 */
	public void append(ASMInstruction instruction, SECTION section) {
		List<ASMInstruction> instructions = Arrays.asList(instruction);
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
		}
	}
	
	/**
	 * Builds this translation unit. This means combining all sections, 
	 * and adding section headers between them.
	 * @return A list of all combined asm instructions.
	 */
	public List<ASMInstruction> buildTranslationUnit() {
		List<ASMInstruction> out = new ArrayList();
		
		long idUsed = this.versionID;
		if (!CompilerDriver.useDefaultVersionID && CompilerDriver.inputFile.getPath().equals(this.sourceFile)) idUsed = 0;
		
		out.add(new ASMDirective(".version " + idUsed));
		out.add(new ASMSeperator());
		
		/* Add imports */
		if (!this.imports.isEmpty()) {
			for (int i = this.imports.size() - 1; i >= 0; i--) 
				out.add(new ASMDirective(".include " + this.imports.get(i)));
			
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
