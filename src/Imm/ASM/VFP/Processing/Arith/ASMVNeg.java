package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.ASMUnaryData;
import Imm.ASM.Util.PRECISION;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.VRegOp;
import Snips.CompilerDriver;

public class ASMVNeg extends ASMUnaryData {
	
	PRECISION precision = PRECISION.F32;
	
	public ASMVNeg(VRegOp target, Operand op1) {
		super(target, op1);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vneg" + 
				((this.cond != null)? this.cond.getCondPostfix() : "" ) + this.precision.toString() + " " + 
				this.target.toString() + ", " + this.op0.toString();
	}

} 
