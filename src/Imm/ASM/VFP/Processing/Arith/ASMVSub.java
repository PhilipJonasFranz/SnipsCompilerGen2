package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMVSub extends ASMSub {

	public ASMVSub(RegOp target, RegOp op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> x - y;
	}
	
	public String build() {
		return super.build("vsub");
	}

} 
