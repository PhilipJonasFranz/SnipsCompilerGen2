package Imm.ASM.Branch;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Snips.CompilerDriver;

public class ASMBranch extends ASMInstruction {

			/* ---< NESTED >--- */
	/** The type of the branch. Corresponds to the notation of the assembly language. */
	public enum BRANCH_TYPE {
	
		B, BL, BX
	
	}
	
	
			/* ---< FIELDS >--- */
	/**
	 * The Type of the branch. See {@link #BRANCH_TYPE}.
	 */
	public BRANCH_TYPE type;
	
	/**
	 * The target of this jump. This can be either a RegOperand, to jump to the contents of
	 * given register, an imm operand, to jump to the value defined in the operand, or a 
	 * label operand to jump to this label.
	 */
	public Operand target;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMBranch(BRANCH_TYPE type, Operand target) {
		this.type = type;
		this.target = target;
	}
	
	public ASMBranch(BRANCH_TYPE type, Cond cond, Operand target) {
		super(cond);
		this.type = type;
		this.target = target;
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		return CompilerDriver.printDepth
				/* Branch type */
				+ this.type.toString().toLowerCase()
				
				/* Condition */
				+ ((this.cond != null)? this.cond.getCondPostfix() : "" )
				
				/* Branch target */
				+ " " + this.target.toString();
	}

} 
