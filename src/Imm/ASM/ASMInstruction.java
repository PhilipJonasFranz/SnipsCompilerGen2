package Imm.ASM;

import Imm.ASM.Util.Cond;

public abstract class ASMInstruction {

	public Cond cond;
	
	public ASMInstruction() {
	}
	
	public ASMInstruction(Cond cond) {
		this.cond = cond;
	}
	
	public abstract String build();
	
}
