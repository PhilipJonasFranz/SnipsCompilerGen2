package Imm.ASM.Util.Operands.Memory;

import Imm.AST.Expression.StructureInit;

public class MemorySkipOperand extends MemoryOperand {

	public StructureInit init;
	
	public MemorySkipOperand(StructureInit init) {
		this.init = init;
	}
	
	public String toString() {
		/* Use the type to string conversion to display type as number */
		return ".skip" + init.type.wordsize() * 4;
	}
	
}
