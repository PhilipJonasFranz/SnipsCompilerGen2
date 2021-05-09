package Imm.ASM.VFP.Memory;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVLdr extends ASMBinaryData {

	public ASMVLdr(RegOp target, Operand op1) {
		super(target, null, op1);
	}
	
	public ASMVLdr(RegOp target, Operand op1, COND cond) {
		/* Op0 is null by default */
		super(target, null, op1, cond);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vldr" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op1.toString();
	}

} 
