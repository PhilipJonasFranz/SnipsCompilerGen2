package Imm.ASM.VFP.Memory;

import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMVLdr extends ASMLdr {

	public ASMVLdr(RegOp target, Operand op1) {
		super(target, op1);
	}
	
	public ASMVLdr(RegOp target, Operand op0, Operand op1) {
		super(target, op0, op1);
	}
	
	public String build() {
		return super.build("vldr");
	}

} 
