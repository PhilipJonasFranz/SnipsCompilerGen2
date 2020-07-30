package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public abstract class ASMUnaryData extends ASMInstruction {

			/* --- NESTED --- */
	/** Used to solve a unary expression for given operand. 
	 * Implementations are defined by classes extending from this class.
	 */
	public interface UnarySolver {
		public int solve(int x);
	}
	
	
			/* --- FIELDS --- */
	public RegOp target;
	
	public Operand op0;
	
	/** The solver for the implementation */
	public UnarySolver solver;
	
	/** Wether to update the condition field when executing this instruction. */
	public boolean updateConditionField = false;
	
	
			/* --- CONSTRUCTORS --- */
	public ASMUnaryData(RegOp target, Operand op0) {
		this.target = target;
		this.op0 = op0;
	}
	
	public ASMUnaryData(RegOp target, Operand op0, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
	}
	
	
			/* --- METHODS --- */
	public abstract String build();

} 
