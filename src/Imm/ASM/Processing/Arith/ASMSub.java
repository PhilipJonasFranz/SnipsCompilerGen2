package Imm.ASM.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Snips.CompilerDriver;

public class ASMSub extends ASMInstruction {

	public Operand target;
	
	public Operand op0;
	
	public Operand op1;
	
	public ASMSub(Operand target, Operand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMSub(Operand target, Operand op0, Operand op1, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "sub" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
	}

}
