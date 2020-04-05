package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public abstract class ASMBinaryData extends ASMInstruction {

			/* --- NESTED --- */
	public interface BinarySolver {
		public int solve(int x, int y);
	}
	
	public enum SHIFT_TYPE {
		LSL, LSR, ASR, ROR;
	}
	
	
			/* --- FIELDS --- */
	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	public BinarySolver solver;
	
	public SHIFT_TYPE shiftType;
	
	public int shiftDist;
	
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
