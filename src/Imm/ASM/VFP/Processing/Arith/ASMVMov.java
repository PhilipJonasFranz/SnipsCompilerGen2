package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.PRECISION;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVMov extends ASMMov {

	PRECISION precision = PRECISION.F32;
	
	public ASMVMov(RegOp target, Operand op1) {
		super(target, op1);
	}
	
	public ASMVMov(RegOp target, Operand op1, COND cond) {
		/* Op0 is null by default */
		super(target, op1, cond);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vmov" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) +
				this.precision.toString() +
				" " + this.target.toString() + ", " + this.op1.toString();
	}

} 
