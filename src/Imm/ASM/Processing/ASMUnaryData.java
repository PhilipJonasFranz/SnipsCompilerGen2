package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.COND;
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
	
	public ASMUnaryData(RegOp target, Operand op0, COND cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
	}
	
	
			/* ---< METHODS >--- */
	public abstract String build();

} 
