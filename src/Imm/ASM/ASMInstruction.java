package Imm.ASM;

import java.util.ArrayList;
import java.util.List;

import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.COND;
import Imm.AsN.AsNNode;

/**
 * Acts as a base class for all assembly instructions.
 */
public abstract class ASMInstruction {

			/* ---< FIELDS >--- */
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
		
		FUNC_CLEAN, STRUCT_INIT,
		
		BX_SEMI_EXIT,
		
		BRANCH_TO_EXIT,
		
		/**
		 * Mark that a jump will target a loop start, or that its jumping upwards, which means
		 * that it can distrupt dataflows.
		 */
		LOOP_BRANCH, 
		
		/**
		 * Applied to labels that are the head of an assembly loop.
		 */
		LOOP_HEAD,
		
		/**
		 * Excludes a label from unused removal.
		 */
		LABEL_USED,
		
		/**
		 * Marks this instructions as offset padding, preventing modification and/or removal.
		 */
		IS_PADDING,
		
		/**
		 * When casting a break or continue statement, a scope pop will be initiated. This will
		 * result in an ASMAdd that adds a value to the SP. When setting this flag, the optimizer
		 * knows that this is not the final loop stack reset, and will not count it to the pushed
		 * words when patching FP to SP.
		 */
		LOOP_BREAK_RESET
	}
	
	/** Flags applied to this asm instruction. See {@link #OPT_FLAG}. */
	public List<OPT_FLAG> optFlags = new ArrayList();
	
	/**
	 * The condition operand of this instruction for conditional instruction execution. 
	 */
	public COND cond;
	
	/**
	 * A comment attatched to this instruction. Will be added after the instruction when building.
	 */
	public ASMComment comment;
	
	/**
	 * The AsNNode that was active when this instruction was created.
	 */
	public AsNNode creator;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMInstruction() {
		if (!AsNNode.creatorStack.isEmpty())
			this.creator = AsNNode.creatorStack.peek();
	}
	
	/**
	 * Constructor for instruction with conditional.
	 */
	public ASMInstruction(COND cond) {
		this.cond = cond;
		
		if (!AsNNode.creatorStack.isEmpty())
			this.creator = AsNNode.creatorStack.peek();
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * Create the ASM memonic representation of this instruction.
	 */
	public abstract String build();
	
	public int getRequiredCPUCycles() {
		return 1;
	}
	
	public boolean isVectorOperation() {
		return this.getClass().getSimpleName().startsWith("ASMV");
	}
	
} 
