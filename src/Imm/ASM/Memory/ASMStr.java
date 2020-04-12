package Imm.ASM.Memory;

import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public class ASMStr extends ASMMemOp {

			/* --- CONSTRUCTORS --- */
	public ASMStr(RegOperand target, Operand op0, Operand op1) {
		super(target, op0, op1);
	}
	
	public ASMStr(RegOperand target, Operand op0) {
		super(target, op0, null);
	}
	
			/* --- METHODS --- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("str");
	}
	
}
