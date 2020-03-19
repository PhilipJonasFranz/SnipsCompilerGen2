package Imm.ASM.Branch;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operand;
import Snips.CompilerDriver;

public class ASMBranch extends ASMInstruction {

	public enum BRANCH_TYPE {
		B, BL, BX
	}
	
	public BRANCH_TYPE type;
	
	public Operand target;
	
	public ASMBranch(BRANCH_TYPE type, Operand target) {
		this.type = type;
		this.target = target;
	}
	
	public String build() {
		return CompilerDriver.printDepth + this.type.toString().toLowerCase() + " " + this.target.toString();
	}

}
