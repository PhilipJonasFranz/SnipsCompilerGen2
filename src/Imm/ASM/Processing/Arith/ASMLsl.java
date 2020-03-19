package Imm.ASM.Processing.Arith;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Snips.CompilerDriver;

public class ASMLsl extends ASMInstruction {

	public Operand op0;
	
	public Operand op1;
	
	public ASMLsl(Operand op0, Operand op1) {
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "lsl " + this.op0.toString() + ", " + this.op1.toString();
	}

}
