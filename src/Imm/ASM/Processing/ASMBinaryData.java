package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Shift;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public abstract class ASMBinaryData extends ASMInstruction {

			/* ---< NESTED >--- */
	/** Used to solve a binary expression between two operands. 
	 * Implementations are defined by classes extending from this class.
	 */
	public interface BinarySolver {

		int solve(int x, int y);

	}
	
	
			/* ---< FIELDS >--- */
	public RegOp target;
	
	public RegOp op0;
	
	public Operand op1;
	
	/** The solver for the implementation */
	public BinarySolver solver;
	
	public Shift shift;
	
	/** Wether to update the condition field when executing this instruction. */
	public boolean updateConditionField = false;
	
	
			/* ---< CONSTRUCTURS >--- */
	public ASMBinaryData(RegOp target, RegOp op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMBinaryData(RegOp target, RegOp op0, Operand op1, COND cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	
			/* ---< METHODS >--- */
	public String build(String operation) {
		String s = CompilerDriver.printDepth + operation + 
				((this.updateConditionField)? "s" : "") + 
				((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + 
				this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
		
		/* Append shift for op1 */
		if (this.shift != null) 
			s += this.shift.getShiftPostfix();
		
		return s;
	}
	
	public String build(String operation, String op_postfix) {
		String s = CompilerDriver.printDepth + operation + 
				((this.updateConditionField)? "s" : "") + 
				((this.cond != null)? this.cond.getCondPostfix() : "" ) + op_postfix + " " + 
				this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
		
		/* Append shift for op1 */
		if (this.shift != null) 
			s += this.shift.getShiftPostfix();
		
		return s;
	}
	
	public void updateCondField() {
		this.updateConditionField = true;
	}
	
	public boolean isUpdatingCondField() {
		return this.updateConditionField;
	}
	
	public int getRequiredCPUCycles() {
		int sum = 1; // +S
		
		if (this.target.reg == REG.PC && this.shift != null) sum = 4; // +I +N +2S
		else if (this.shift != null) sum = 2; // +I +S
		else if (this.target.reg == REG.PC) sum = 3; // +N +2S
		
		return sum;
	}

} 
