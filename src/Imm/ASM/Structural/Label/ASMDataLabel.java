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
		return super.build() + this.memoryOperand.toString();
	}
	
	public ASMDataLabel clone() {
		return new ASMDataLabel(this.name, this.memoryOperand.clone());
	}
	
	public int getRequiredCPUCycles() {
		return 0;
	}
	
} 
