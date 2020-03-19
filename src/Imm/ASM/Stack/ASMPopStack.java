package Imm.ASM.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operand;
import Imm.ASM.Util.RegOperand;
import Snips.CompilerDriver;

public class ASMPopStack extends ASMInstruction {

	public Operand [] operands;
	
	public ASMPopStack(RegOperand...operands) {
		this.operands = operands;
	}

	public String build() {
		String s = CompilerDriver.printDepth + "pop {";
		for (int i = 0; i < operands.length; i++) {
			s += operands [i].toString();
			if (i < operands.length - 1) s += ", ";
		}
		s += "}";
		return s;
	}
	
}
