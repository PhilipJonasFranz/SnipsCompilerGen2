package Imm.ASM.Util.Operands;

import Imm.ASM.Structural.Label.ASMLabel;

public class LabelOp extends Operand {

			/* --- FIELDS --- */
	/**
	 * The ASM Label this operand points to.
	 */
	public ASMLabel label;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default Constructor. Needs patching via {@link #patch(ASMLabel)} to work properly.
	 */
	public LabelOp() {
		
	}
	
	/**
	 * Create a new Label Operand that points to given label.
	 */
	public LabelOp(ASMLabel label) {
		this.label = label;
	}
	
	
			/* --- METHODS --- */
	/**
	 * Set the {@link #label} to given label.
	 */
	public void patch(ASMLabel label) {
		this.label = label;
	}

	/**
	 * Return the name of the label.
	 */
	public String toString() {
		return this.label.name;
	}

	public LabelOp clone() {
		return new LabelOp(this.label);
	}

	public boolean equals(Operand operand) {
		return operand instanceof LabelOp && ((LabelOp) operand).label.equals(this.label);
	}
	
} 
