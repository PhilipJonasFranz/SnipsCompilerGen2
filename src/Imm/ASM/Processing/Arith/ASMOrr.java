package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public class ASMOrr extends ASMBinaryData {
	
	public ASMOrr(RegOperand target, RegOperand op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> (x == 1 || y == 1)? 1 : 0;
	}
	
	public ASMOrr(RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(target, op0, op1, cond);
		this.solver = (x, y) -> x + y;
	}
	
	public String build() {
		return super.build("orr");
	}

}
