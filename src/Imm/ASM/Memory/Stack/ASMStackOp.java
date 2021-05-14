package Imm.ASM.Memory.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public abstract class ASMStackOp extends ASMInstruction {

			/* ---< NESTED >--- */
	/** Used to describe the writeback and indexing behaviour */
	public enum MEM_OP {
		PRE_WRITEBACK, 
		POST_WRITEBACK,
		PRE_NO_WRITEBACK
	}
	
	
			/* ---< FIELDS >--- */
	/** Writeback and indexing behaviour */
	public MEM_OP memOp;
	
	public RegOp target;
	
	public RegOp op0;
	
	public Operand op1;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMStackOp(MEM_OP memOp, RegOp target, RegOp op0, Operand op1) {
		this.memOp = memOp;
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}
	
	
			/* ---< METHODS >--- */
	public String build(String operation) {
		operation += " ";
		if (this.memOp == MEM_OP.POST_WRITEBACK) {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
				this.op0.toString() + "]";
			if (this.op1 != null) {
				s += " " + this.op1;
			}
			return s;
		}
		else if (this.memOp == MEM_OP.PRE_WRITEBACK) {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
				this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1 + "]!";
			}
			else s += "]";
			return s;
		}
		else {
			String s = CompilerDriver.printDepth + operation + this.target.toString() + ", [" +
					this.op0.toString();
			if (this.op1 != null) {
				s += ", " + this.op1;
			}
			s += "]";
			return s;
		}
	}
	
} 
