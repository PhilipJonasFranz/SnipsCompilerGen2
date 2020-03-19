package Imm.ASM.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMLsr extends ASMInstruction {

	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	public ASMLsr(RegOperand target, RegOperand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		if (this.target.reg == this.op0.reg) {
			return CompilerDriver.printDepth + "lsr " + this.op0.toString() + ", " + this.op1.toString();
		}
		else {
			return CompilerDriver.printDepth + "lsr " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
		}
	}

}
