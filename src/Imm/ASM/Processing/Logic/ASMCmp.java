package Imm.ASM.Processing.Logic;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMCmp extends ASMInstruction {

	public RegOp op0;
	
	public Operand op1;
	
	public ASMCmp(RegOp op0, Operand op1) {
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "cmp" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.op0.toString() + ", " + this.op1.toString();
	}
	
	public int getRequiredCPUCycles() {
		return 1; // +S
	}

} 
