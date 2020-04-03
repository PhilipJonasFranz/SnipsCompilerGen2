package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Structural.Label.ASMDataLabel;

public class MemoryWordRefOperand extends MemoryOperand {

	public ASMDataLabel dataLabel;
	
	public MemoryWordRefOperand(ASMDataLabel dataLabel) {
		this.dataLabel = dataLabel;
	}
	
	public String toString() {
		return ".word " + this.dataLabel.name;
	}
	
}
