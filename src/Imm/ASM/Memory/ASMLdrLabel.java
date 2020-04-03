package Imm.ASM.Memory;

import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMLdrLabel extends ASMLdr {

			/* --- CONSTRUCTORS --- */
	public ASMLdrLabel(RegOperand target, LabelOperand op0) {
		super(target, op0, null);
	}
	
			/* --- METHODS --- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	@Override
	public String build() {
		String s = CompilerDriver.printDepth + "ldr " + target.toString();
		s += ", " + this.op0.toString();
		return s;
	}
	
}
