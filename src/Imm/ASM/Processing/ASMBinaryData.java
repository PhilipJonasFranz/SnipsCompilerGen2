package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public abstract class ASMBinaryData extends ASMInstruction {

			/* --- NESTED --- */
	/** Used to solve a binary expression between two operands. 
	 * Implementations are defined by classes extending from this class.
	 */
	public interface BinarySolver {
		public int solve(int x, int y);
	}
	
	/** Used ot describe the type of the operand shift */
	public enum SHIFT_TYPE {
		LSL, LSR, ASR, ROR;
	}
	
	
			/* --- FIELDS --- */
	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	/** The solver for the implementation */
	public BinarySolver solver;
	
	/** The shift type. If set to null, no shift is applied. */
	public SHIFT_TYPE shiftType;
	
	/** The amount of shift that is applied. */
	public int shiftDist;
	
	/** Wether to update the condition field when executing this instruction. */
	public boolean updateConditionField = false;
	
	
			/* --- CONSTRUCTURS --- */
	public ASMBinaryData(RegOperand target, RegOperand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMBinaryData(RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	
			/* --- METHODS --- */
	public String build(String operation) {
		String s = CompilerDriver.printDepth + operation + ((this.updateConditionField)? "s" : "") + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.op0.toString() + ", " + this.op1.toString();
		if (this.shiftType != null) {
			s += ", " + this.shiftType.toString().toLowerCase() + " #" + this.shiftDist;
		}
		return s;
	}

}
