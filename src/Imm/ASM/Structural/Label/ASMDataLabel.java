package Imm.ASM.Structural.Label;

import Imm.ASM.Util.Operands.Memory.MemoryOperand;

/**
 * Describes f.E. the ASM Line: [name]: .word [variableName]
 */
public class ASMDataLabel extends ASMLabel {

	public MemoryOperand memoryOperand;
	
	public ASMDataLabel(String name, MemoryOperand memoryOperand) {
		super(name);
		this.memoryOperand = memoryOperand;
	}
	
	public String build() {
		return this.name + ": " + this.memoryOperand.toString();
	}
	
}
