package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;

public class ASMComment extends ASMInstruction {

	String comment;
	
	public ASMComment(String comment) {
		this.comment = comment;
	}

	public String build() {
		return "/* " + this.comment + " */";
	}
	
}
