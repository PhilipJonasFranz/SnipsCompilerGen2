package Imm.ASM.Memory;

import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;

public class ASMLdr extends ASMMemOp {

			/* ---< CONSTRUCTORS >--- */
	public ASMLdr(RegOp target, Operand op0, Operand op1) {
		super(target, op0, op1);
	}
	
	public ASMLdr(RegOp target, Operand op0) {
		super(target, op0, null);
	}
	
			/* ---< METHODS >--- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("ldr");
	}
	
	public int getRequiredCPUCycles() {
		if (this.target.reg == REG.PC) return 5; // +N +I +N + 2S
		else return 3; // +N +I +S
	}
	
} 
