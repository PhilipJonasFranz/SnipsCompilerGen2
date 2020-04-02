package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMRsb extends ASMBinaryData {
	
	public ASMRsb(RegOperand target, RegOperand op0, Operand op1) {
		super(target, op0, op1);
		this.solver = (x, y) -> y - x;
	}
	
	public ASMRsb(RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(target, op0, op1, cond);
		this.solver = (x, y) -> y - x;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "rsb" + ((this.updateConditionField)? "s" : "") + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
	}

}
