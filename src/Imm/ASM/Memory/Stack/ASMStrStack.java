package Imm.ASM.Memory.Stack;

import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMStrStack extends ASMStackOp {

			/* ---< CONSTRUCTORS >--- */
	public ASMStrStack(MEM_OP memOp, RegOp target, RegOp op0, Operand op1) {
		super(memOp, target, op0, op1);
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("str");
	}
	
	public int getRequiredCPUCycles() {
		return 2; // +N +N
	}
	
} 
