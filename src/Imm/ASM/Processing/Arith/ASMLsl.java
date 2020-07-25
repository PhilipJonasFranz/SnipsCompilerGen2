package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMLsl extends ASMBinaryData {

	public ASMLsl(RegOp target, RegOp op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> x << y;
	}
	
	public ASMLsl(RegOp target, RegOp op0, Operand op1, Cond cond) {
		super(target, op0, op1, cond);
		this.solver = (x, y) -> x << y;
	}
	
	public String build() {
		return super.build("lsl");
	}

}
