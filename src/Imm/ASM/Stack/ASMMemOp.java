package Imm.ASM.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public abstract class ASMMemOp extends ASMInstruction {

	public enum MEM_OP {
		PRE_WRITEBACK, 
		POST_WRITEBACK,
		PRE_NO_WRITEBACK
	}
	
	public MEM_OP memOp;
	
	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	public ASMMemOp(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1) {
		this.memOp = memOp;
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	public ASMMemOp(MEM_OP memOp, RegOperand target, RegOperand op0, Operand op1, Cond cond) {
		super(cond);
		this.memOp = memOp;
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}

	public abstract String build();
	
}
