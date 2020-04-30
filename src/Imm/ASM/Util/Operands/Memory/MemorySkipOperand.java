package Imm.ASM.Util.Operands.Memory;

import Imm.AST.Expression.ArrayInit;

public class MemorySkipOperand extends MemoryOperand {

	public ArrayInit init;
	
	public MemorySkipOperand(ArrayInit init) {
		this.init = init;
	}
	
	public String toString() {
		/* Use the type to string conversion to display type as number */
		return ".skip" + init.getType().wordsize() * 4;
	}

	public MemoryOperand clone() {
		return new MemorySkipOperand(this.init);
	}
	
}
