package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;

public class ASMSectionAnnotation extends ASMInstruction {

	public enum SECTION {
		DATA, TEXT, GLOBAL
	}
	
	public SECTION section;
	
	public String postfix;
	
	public ASMSectionAnnotation(SECTION section) {
		this.section = section;
	}
	
	public ASMSectionAnnotation(SECTION section, String postfix) {
		this.section = section;
		this.postfix = postfix;
	}

	public String build() {
		return "." + this.section.toString().toLowerCase() + ((this.postfix != null)? " " + this.postfix : "");
	}
	
}
