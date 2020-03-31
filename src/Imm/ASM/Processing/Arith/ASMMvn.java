package Imm.ASM.Processing.Arith;

import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMMvn extends ASMBinaryData {

	public ASMMvn(RegOperand target, Operand origin) {
		super(target, null, origin);
	}
	
	public ASMMvn(RegOperand target, Operand origin, Cond cond) {
		super(target, null, origin, cond);
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mvn" + ((this.updateConditionField)? "s" : "") + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op1.toString();
	}

}