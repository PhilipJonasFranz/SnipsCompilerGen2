package Imm.ASM.Stack;

import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public class ASMStrStack extends ASMMemOp {

	public ASMStrStack(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1) {
		super(memOp, target, op0, op1);
	}
	
	public ASMStrStack(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(memOp, target, op0, op1, cond);
	}

			/* --- METHODS --- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	@Override
	public String build() {
		return super.build(this.getClass());
	}
	
}
