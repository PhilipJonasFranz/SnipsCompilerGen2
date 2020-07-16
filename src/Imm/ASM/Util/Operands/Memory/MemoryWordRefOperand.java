package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.Operand;

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

	public boolean equals(Operand operand) {
		if (!(operand instanceof MemoryWordRefOperand)) return false;
		else {
			MemoryWordRefOperand op = (MemoryWordRefOperand) operand;
			return op.prefix.equals(this.prefix) && op.dataLabel.equals(this.dataLabel);
		}
	}
	
}
