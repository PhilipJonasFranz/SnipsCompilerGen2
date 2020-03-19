package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operand;
import Snips.CompilerDriver;

public class ASMMult extends ASMInstruction {

	public Operand target;
	
	public Operand op0;
	
	public Operand op1;
	
	public ASMMult(Operand target, Operand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mul " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
	}

}
