package Imm.ASM.Memory;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public abstract class ASMMemOp extends ASMInstruction {

			/* --- FIELDS --- */
	public RegOperand target;
	
	public Operand op0;
	
	public Operand op1;
	
	
			/* --- CONSTRUCTORS --- */
	public ASMMemOp(RegOperand target, Operand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMMemOp(RegOperand target, Operand op0, Operand op1, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	
			/* --- METHODS --- */
	public String build(String operation) {
		operation += " ";
		String s = CompilerDriver.printDepth + operation + target.toString();
		if (op1 != null) {
			s += ", [" + this.op0.toString() + ", " + this.op1.toString() + "]";
		}
		else s += ", [" + this.op0.toString() + "]";
		
		return s;
	}
	
}
