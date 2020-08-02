package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.Operand;

public class MemoryWordRefOp extends MemoryOperand {

	public String prefix = "";
	
	public ASMDataLabel dataLabel;
	
	public MemoryWordRefOp(ASMDataLabel dataLabel) {
		this.dataLabel = dataLabel;
	}
	
	public String toString() {
		return ".word " + this.prefix + this.dataLabel.name;
	}

	public MemoryOperand clone() {
		return new MemoryWordRefOp(this.dataLabel.clone());
	}

	public boolean equals(Operand operand) {
		if (!(operand instanceof MemoryWordRefOp)) return false;
		else {
			MemoryWordRefOp op = (MemoryWordRefOp) operand;
			return op.prefix.equals(this.prefix) && op.dataLabel.equals(this.dataLabel);
		}
	}
	
} 
