package Imm.ASM.Memory.Stack;

import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMLdrStack extends ASMStackOp {

			/* --- CONSTRUCTORS --- */
	public ASMLdrStack(MEM_OP memOp, RegOp target, RegOp op0, Operand op1) {
		super(memOp, target, op0, op1);
	}
	
	public ASMLdrStack(MEM_OP memOp, RegOp target, RegOp op0, Operand op1, Cond cond) {
		super(memOp, target, op0, op1, cond);
	}

	
			/* --- METHODS --- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("ldr");
	}
	
} 
