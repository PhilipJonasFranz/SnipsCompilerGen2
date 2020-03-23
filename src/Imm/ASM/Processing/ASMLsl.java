package Imm.ASM.Processing;

import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMLsl extends ASMDataP {

	public ASMLsl(RegOperand target, RegOperand op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> x << y;
	}
	
	public ASMLsl(RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(target, op0, op1, cond);
		this.solver = (x, y) -> x << y;
	}
	
	public String build() {
		if (this.target.reg == this.op0.reg) {
			return CompilerDriver.printDepth + "lsl" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.op0.toString() + ", " + this.op1.toString();
		}
		else {
			return CompilerDriver.printDepth + "lsl" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
		}
	}

}
