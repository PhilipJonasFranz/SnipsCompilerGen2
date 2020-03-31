package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public abstract class ASMBinaryData extends ASMInstruction {

	public interface BinarySolver {
		public int solve(int x, int y);
	}
	
	public BinarySolver solver;
	
	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	public boolean updateConditionField = false;
	
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
	
	public abstract String build();

}
