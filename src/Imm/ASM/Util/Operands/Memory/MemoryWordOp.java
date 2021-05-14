package Imm.ASM.Util.Operands.Memory;

import Imm.ASM.Util.Operands.Operand;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.PRIMITIVES.CHAR;

public class MemoryWordOp extends MemoryOperand {

	public Expression value;
	
	public int intValue;
	
	public MemoryWordOp(Expression expr) {
		this.value = expr;
	}
	
	public MemoryWordOp(int value) {
		this.intValue = value;
	}
	
	public String toString() {
		if (this.value != null) {
			/* Use the type to string conversion to display type as number */
			return this.toString(value);
		}
		else {
			/* Simple integer value */
			return ".word " + this.intValue;
		}
	}
	
	private String toString(Expression val) {
		if (val instanceof Atom atom)
			return ".word " + atom.getType().toPrimitive().sourceCodeRepresentation();

		if (val instanceof ArrayInit init && ((ARRAY) val.getType()).elementType instanceof CHAR) {
			if (init.elements.stream().allMatch(x -> x instanceof Atom)) {
				String s = ".asciz \"";
				
				for (int i = 0; i < init.elements.size() - 1; i++) {
					Atom atom = (Atom) init.elements.get(i);
					s += (char) Integer.parseInt(atom.getType().toPrimitive().sourceCodeRepresentation());
				}
				
				s += "\"";
				
				return s;
			}
		}
		
		return ".skip " + val.getType().wordsize() * 4;
	}

	public MemoryOperand clone() {
		MemoryWordOp clone = new MemoryWordOp(this.value);
		clone.intValue = this.intValue;
		return clone;
	}

	public boolean equals(Operand operand) {
		return false;
	}
	
} 
