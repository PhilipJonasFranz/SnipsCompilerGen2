package Imm.ASM.Memory;

import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public class ASMLdr extends ASMMemOp {

			/* --- CONSTRUCTORS --- */
	public ASMLdr(RegOperand target, Operand op0) {
		super(target, op0);
	}
	
			/* --- METHODS --- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	@Override
	public String build() {
		return super.build("ldr");
	}
	
}
