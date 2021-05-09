package Imm.ASM.VFP.Memory;

import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVLdr extends ASMLdr {

	public ASMVLdr(RegOp target, Operand op1) {
		super(target, null, op1);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "vldr" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op1.toString();
	}

} 
