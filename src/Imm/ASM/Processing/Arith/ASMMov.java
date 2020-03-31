package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMMov extends ASMBinaryData {

	public ASMMov(RegOperand target, Operand op1) {
		super(target, null, op1);
	}
	
	public ASMMov(RegOperand target, Operand op1, Cond cond) {
		/* Op0 is null by default */
		super(target, null, op1, cond);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mov" + ((this.updateConditionField)? "s" : "") + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op1.toString();
	}

}