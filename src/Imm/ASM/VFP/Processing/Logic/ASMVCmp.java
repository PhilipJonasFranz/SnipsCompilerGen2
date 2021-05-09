package Imm.ASM.VFP.Processing.Logic;

import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVCmp extends ASMCmp {

	public ASMVCmp(RegOp op0, Operand op1) {
		super(op0, op1);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vcmp" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.op0.toString() + ", " + this.op1.toString();
	}
	
} 
