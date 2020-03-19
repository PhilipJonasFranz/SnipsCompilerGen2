package Imm.ASM.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMPushStack extends ASMInstruction {

	public Operand [] operands;
	
	public ASMPushStack(RegOperand...operands) {
		this.operands = operands;
	}

	public String build() {
		String s = CompilerDriver.printDepth + "push {";
		for (int i = 0; i < operands.length; i++) {
			s += operands [i].toString();
			if (i < operands.length - 1) s += ", ";
		}
		s += "}";
		return s;
	}
	
}
