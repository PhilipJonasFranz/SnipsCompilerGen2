package Imm.ASM.Stack;

import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMStrStack extends ASMMemOp {

	public ASMStrStack(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1) {
		super(memOp, target, op0, op1);
	}
	
	public ASMStrStack(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(memOp, target, op0, op1, cond);
	}

	public String build() {
		if (this.memOp == MEM_OP.POST_WRITEBACK) {
			String s = CompilerDriver.printDepth + "str " + this.target.toString() + ", [" +
				this.op0.toString() + "]";
			if (this.op1 != null) {
				s += " " + this.op1.toString();
			}
			return s;
		}
		else if (this.memOp == MEM_OP.PRE_WRITEBACK) {
			String s = CompilerDriver.printDepth + "str " + this.target.toString() + ", [" +
				this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1.toString() + "]!";
			}
			else s += "]";
			return s;
		}
		else {
			String s = CompilerDriver.printDepth + "str " + this.target.toString() + ", [" +
					this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1.toString();
			}
			s += "]";
			return s;
		}
	}
	
}
