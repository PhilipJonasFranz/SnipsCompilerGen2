package Imm.ASM.Util.Operands;

import Imm.ASM.Structural.ASMLabel;

public class LabelOperand extends Operand {

			/* --- FIELDS --- */
	/**
	 * The ASM Label this operand points to.
	 */
	public ASMLabel label;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default Constructor. Needs patching via {@link #patch(ASMLabel)} to work properly.
	 */
	public LabelOperand() {
		
	}
	
	/**
	 * Create a new Label Operand that points to given label.
	 */
	public LabelOperand(ASMLabel label) {
		this.label = label;
	}
	
	
			/* --- METHODS --- */
	/**
	 * Set the {@link #label} to given label.
	 */
	public void patch(ASMLabel label) {
		this.label = label;
	}

	@Override
	/**
	 * Return the name of the label.
	 */
	public String toString() {
		return this.label.name;
	}
	
}
