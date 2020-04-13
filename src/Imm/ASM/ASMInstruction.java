package Imm.ASM;

import java.util.HashMap;

import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Cond;

/**
 * Acts as a base class for all assembly instructions.
 */
public abstract class ASMInstruction {

			/* --- FIELDS --- */
	public enum OPT_FLAG {
		WRITEBACK;
	}
	
	public HashMap<OPT_FLAG, Boolean> optFlags = new HashMap();
	
	/**
	 * The condition operand of this instruction for conditional instruction execution. 
	 */
	public Cond cond;
	
	/**
	 * A comment attatched to this instruction. Will be added after the instruction when building.
	 */
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
