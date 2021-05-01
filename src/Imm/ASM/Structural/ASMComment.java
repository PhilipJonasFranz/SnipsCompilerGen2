package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;
import Snips.CompilerDriver;

public class ASMComment extends ASMInstruction {

	public String comment;
	
	public ASMComment(String comment) {
		this.comment = comment;
	}

	public String build() {
		return "/* " + this.comment + " */";
	}
	
	public String build(int x) {
		String s = "";
		for (int i = 0; i < CompilerDriver.commentDistance - x; i++) s+= " ";
		return s + "/* " + this.comment + " */";
	}
	
	public int getRequiredCPUCycles() {
		return 0;
	}
	
} 
