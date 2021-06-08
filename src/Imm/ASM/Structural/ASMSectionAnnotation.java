package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;

public class ASMSectionAnnotation extends ASMInstruction {

	public enum SECTION {

		DATA, TEXT, GLOBAL;

		public String toString() {
			return "." + this.name().toLowerCase();
		}

	}
	
	public SECTION section;

	public ASMSectionAnnotation(SECTION section) {
		this.section = section;
	}

	public String build() {
		return this.section.toString();
	}

	public int getRequiredCPUCycles() {
		return 0;
	}
	
} 
