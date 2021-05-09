package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVMov extends ASMBinaryData {

	public ASMVMov(RegOp target, Operand op1) {
		super(target, null, op1);
	}
	
	public ASMVMov(RegOp target, Operand op1, COND cond) {
		/* Op0 is null by default */
		super(target, null, op1, cond);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vmov" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op1.toString();
	}

} 
