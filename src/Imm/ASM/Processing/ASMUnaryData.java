package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public abstract class ASMUnaryData extends ASMInstruction {

			/* --- NESTED --- */
	public interface UnarySolver {
		public int solve(int x);
	}
	
	
			/* --- FIELDS --- */
	public RegOperand target;
	
	public Operand op0;
	
	public UnarySolver solver;
	
	public boolean updateConditionField = false;
	
	
			/* --- CONSTRUCTORS --- */
	public ASMUnaryData(RegOperand target, Operand op0) {
		this.target = target;
		this.op0 = op0;
	}
	
	public ASMUnaryData(RegOperand target, Operand op0, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
	}
	
	
			/* --- METHODS --- */
	public abstract String build();

}
