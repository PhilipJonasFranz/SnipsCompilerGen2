package Imm.ASM.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMMla extends ASMInstruction {

	public RegOp target;
	
	public RegOp op0;
	
	public RegOp op1;
	
	public RegOp op2;
	
	public ASMMla(RegOp target, RegOp op0, RegOp op1, RegOp op2) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
		this.op2 = op2;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mla" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString() + ", " + this.op2.toString();
	}

} 
