package Imm.ASM.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMPushStack extends ASMInstruction {

	public Operand [] operands;
	
	public ASMPushStack(RegOperand...operands) {
		this.operands = operands;
	}
	
	public ASMPushStack(Cond cond, RegOperand...operands) {
		super(cond);
		this.operands = operands;
	}

	public String build() {
		String s = CompilerDriver.printDepth + "push" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " { ";
		for (int i = 0; i < operands.length; i++) {
			s += operands [i].toString();
			if (i < operands.length - 1) s += ", ";
		}
		s += " }";
		return s;
	}
	
}
