package Imm.ASM.Branch;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Snips.CompilerDriver;

public class ASMBranch extends ASMInstruction {

	public enum BRANCH_TYPE {
		B, BL, BX
	}
	
	public BRANCH_TYPE type;
	
	public Operand target;
	
	public Cond cond;
	
	public ASMBranch(BRANCH_TYPE type, Operand target) {
		this.type = type;
		this.target = target;
	}
	
	public ASMBranch(BRANCH_TYPE type, Cond cond, Operand target) {
		this.type = type;
		this.target = target;
		this.cond = cond;
	}
	
	public String build() {
		return CompilerDriver.printDepth + this.type.toString().toLowerCase() + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString();
	}

}
