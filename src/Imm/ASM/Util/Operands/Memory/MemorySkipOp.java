package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Util.Operands.Operand;
import Imm.AST.Expression.ArrayInit;

/**
 * Leaves out a free memory section in the .data section, 
 * based on the size of the given array init type word size.
 */
public class MemorySkipOp extends MemoryOperand {

	public ArrayInit init;
	
	public MemorySkipOp(ArrayInit init) {
		this.init = init;
	}
	
	public String toString() {
		/* Use the type to string conversion to display type as number */
		return ".skip" + init.getType().wordsize() * 4;
	}

	public MemoryOperand clone() {
		return new MemorySkipOp(this.init);
	}

	public boolean equals(Operand operand) {
		if (!(operand instanceof MemorySkipOp)) return false;
		else {
			return ((MemorySkipOp) operand).init.equals(this.init);
		}
	}
	
} 
