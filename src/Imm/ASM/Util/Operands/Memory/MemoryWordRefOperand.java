package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Structural.Label.ASMDataLabel;

public class MemoryWordRefOperand extends MemoryOperand {

	public String prefix = "";
	
	public ASMDataLabel dataLabel;
	
	public MemoryWordRefOperand(ASMDataLabel dataLabel) {
		this.dataLabel = dataLabel;
	}
	
	public String toString() {
		return ".word " + this.prefix + this.dataLabel.name;
	}

	public MemoryOperand clone() {
		return new MemoryWordRefOperand(this.dataLabel.clone());
	}
	
}
