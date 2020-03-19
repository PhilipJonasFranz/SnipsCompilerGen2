package Imm.ASM.Util;

import Imm.ASM.Structural.ASMLabel;

public class LabelOperand extends Operand {

	public ASMLabel label;
	
	public LabelOperand() {
		
	}
	
	public LabelOperand(ASMLabel label) {
		this.label = label;
	}
	
	public void patch(ASMLabel label) {
		this.label = label;
	}

	public String toString() {
		return this.label.name;
	}
	
}
