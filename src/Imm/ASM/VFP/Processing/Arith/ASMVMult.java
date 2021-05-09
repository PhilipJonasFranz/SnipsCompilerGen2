package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVMult extends ASMInstruction {

	public RegOp target;
	
	public RegOp op0;
	
	public RegOp op1;
	
	public ASMVMult(RegOp target, RegOp op0, RegOp op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vmul" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
	}
	
} 
