package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public abstract class ASMUnaryData extends ASMInstruction {
	
			/* ---< FIELDS >--- */
	public RegOp target;
	
	public Operand op0;
	
	/** Wether to update the condition field when executing this instruction. */
	public boolean updateConditionField = false;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMUnaryData(RegOp target, Operand op0) {
		this.target = target;
		this.op0 = op0;
	}


	/* ---< METHODS >--- */
	public abstract String build();
	
	public int getRequiredCPUCycles() {
		int sum = 1; // +S
		
		if (this.target.reg == REG.PC) sum = 3; // +N +2S
		
		return sum;
	}

} 
