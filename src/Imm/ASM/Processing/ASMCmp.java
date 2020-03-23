package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMCmp extends ASMInstruction {

	public RegOperand op0;
	
	public Operand op1;
	
	public ASMCmp(RegOperand op0, Operand op1) {
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMCmp(RegOperand op0, Operand op1, Cond cond) {
		super(cond);
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "cmp" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.op0.toString() + ", " + this.op1.toString();
	}

}
