package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.PRECISION;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVCvt extends ASMBinaryData {
	
	public PRECISION from;
	
	public PRECISION to;
	
	
	public ASMVCvt(RegOp target, RegOp op1, PRECISION from, PRECISION to) {
		super(target, null, op1);
		this.from = from;
		this.to = to;
	}
	
	public ASMVCvt(RegOp target, RegOp op1, PRECISION from, PRECISION to, COND cond) {
		/* Op0 is null by default */
		super(target, null, op1, cond);
		this.from = from;
		this.to = to;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vcvt" + 
				((this.cond != null)? this.cond.getCondPostfix() : "" ) +
				this.from.toString() + this.to.toString() + " " + 
				this.target.toString() + ", " + 
				this.op1.toString();
	}

} 
