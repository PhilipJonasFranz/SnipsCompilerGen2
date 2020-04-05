package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public class ASMSub extends ASMBinaryData {

	public ASMSub(RegOperand target, RegOperand op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> x - y;
	}
	
	public ASMSub(RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(target, op0, op1, cond);
		this.solver = (x, y) -> x - y;
	}
	
	public String build() {
		return super.build("sub");
	}

}
