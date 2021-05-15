package Imm.ASM.VFP.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.Util.PRECISION;

public class ASMVDiv extends ASMBinaryData {

	PRECISION precision = PRECISION.F32;

	public ASMVDiv(VRegOp target, VRegOp op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> x / y;
	}
	
	public String build() {
		return super.build("vdiv", this.precision.toString());
	}

} 
