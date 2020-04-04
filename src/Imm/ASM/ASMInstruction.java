package Imm.ASM;

import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Cond;

public abstract class ASMInstruction {

			/* --- FIELDS --- */
	/**
	 * The condition operand of this instruction for conditional instruction execution. 
	 */
	public Cond cond;
	
	public ASMComment comment;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default Constructor.
	 */
	public ASMInstruction() {
	}
	
	/**
	 * Constructor for instruction with conditional.
	 */
	public ASMInstruction(Cond cond) {
		this.cond = cond;
	}
	
	
			/* --- METHODS --- */
	/**
	 * Create the ASM memonic representation of this instruction.
	 * @return
	 */
	public abstract String build();
	
}
