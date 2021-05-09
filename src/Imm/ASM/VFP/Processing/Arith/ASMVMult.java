package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVMult extends ASMMult {

	public ASMVMult(RegOp target, RegOp op0, RegOp op1) {
		super(target, op0, op1);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vmul" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
	}
	
} 
