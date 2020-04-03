package Imm.ASM.Util.Operands.Memory;

import Imm.AST.Expression.Atom;

public class MemoryWordOperand extends MemoryOperand {

	public Atom value;
	
	public MemoryWordOperand(Atom atom) {
		this.value = atom;
	}
	
	public String toString() {
		/* Use the type to string conversion to display type as number */
		return ".word " + this.value.type.sourceCodeRepresentation();
	}
	
}
