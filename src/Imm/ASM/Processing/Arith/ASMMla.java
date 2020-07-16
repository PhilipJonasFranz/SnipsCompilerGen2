package Imm.ASM.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMMla extends ASMInstruction {

	public RegOperand target;
	
	public RegOperand op0;
	
	public RegOperand op1;
	
	public RegOperand op2;
	
	public ASMMla(RegOperand target, RegOperand op0, RegOperand op1, RegOperand op2) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
		this.op2 = op2;
	}
	
	public ASMMla(RegOperand target, RegOperand op0, RegOperand op1, RegOperand op2, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
		this.op2 = op2;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mla" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString() + ", " + this.op2.toString();
	}

}
