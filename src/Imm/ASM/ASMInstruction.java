package Imm.ASM;

import java.util.ArrayList;
import java.util.List;

import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Cond;

/**
 * Acts as a base class for all assembly instructions.
 */
public abstract class ASMInstruction {

			/* --- FIELDS --- */
	/** 
	 * Flags that can be applied to {@link #optFlags} to interfere with the asm optimizer. 
	 */
	public enum OPT_FLAG {
		/** 
		 * This flag is set if a binary expression is part of a increment or decrement operation.
		 *  Setting this flag will prevent the optimizer to fuze statements<br>
		 *  mov r0, rx<br>
		 *  add r1, r0, #1<br>
		 *  into<br>
		 *  add r1, rx, #1<br>
		 */
		WRITEBACK,
		
		EXC_EXIT,
		
		/**
		 * This flag is set to signal that a 'b' jump is a controlled jump, and the program
		 * will return to the jump location by manipulating the pc. This is used to prevent
		 * the optimizer from deleting instructions after the jump.
		 */
		SYS_JMP,
		
		FUNC_CLEAN;
	}
	
	/** Flags applied to this asm instruction. See {@link #OPT_FLAG}. */
	public List<OPT_FLAG> optFlags = new ArrayList();
	
	/**
	 * The condition operand of this instruction for conditional instruction execution. 
	 */
	public Cond cond;
	
	/**
	 * A comment attatched to this instruction. Will be added after the instruction when building.
	 */
	public ASMComment comment;
	
	
			/* --- CONSTRUCTORS --- */
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
