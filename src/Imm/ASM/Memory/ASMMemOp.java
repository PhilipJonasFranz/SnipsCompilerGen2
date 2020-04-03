package Imm.ASM.Memory;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public abstract class ASMMemOp extends ASMInstruction {

			/* --- NESTED --- */
	public enum MEM_OP {
		PRE_WRITEBACK, 
		POST_WRITEBACK,
		PRE_NO_WRITEBACK
	}
	
	
			/* --- FIELDS --- */
	public MEM_OP memOp;
	
	public RegOperand target;
	
	public RegOperand op0;
	
	public Operand op1;
	
	
			/* --- CONSTRUCTORS --- */
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
	
	public ASMMemOp(RegOperand target, Operand op) {
		this.target = target;
		this.op1 = op;
	}

	
			/* --- METHODS --- */
	public String build(String operation) {
		operation += " ";
		if (this.memOp == null) {
			/* Load Label */
			if (this.op1 instanceof LabelOperand) {
				String s = CompilerDriver.printDepth + operation + this.target.toString() + ", " +
					this.op1.toString();
				return s;
			}
			else {
				String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
					this.op1.toString() + "]";
				return s;
			}
		}
		else if (this.memOp == MEM_OP.POST_WRITEBACK) {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
				this.op0.toString() + "]";
			if (this.op1 != null) {
				s += " " + this.op1.toString();
			}
			return s;
		}
		else if (this.memOp == MEM_OP.PRE_WRITEBACK) {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
				this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1.toString() + "]!";
			}
			else s += "]";
			return s;
		}
		else {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
					this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1.toString();
			}
			s += "]";
			return s;
		}
	}
	
}
