package Imm.ASM.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMPopStack extends ASMInstruction {

	public Operand [] operands;
	
	public ASMPopStack(RegOperand...operands) {
		this.operands = operands;
	}
	
	public ASMPopStack(Cond cond, RegOperand...operands) {
		super(cond);
		this.operands = operands;
	}

	public String build() {
		String s = CompilerDriver.printDepth + "pop" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " { ";
		for (int i = 0; i < operands.length; i++) {
			s += operands [i].toString();
			if (i < operands.length - 1) s += ", ";
		}
		s += " }";
		return s;
	}
	
}
