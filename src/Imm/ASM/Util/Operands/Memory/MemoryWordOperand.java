package Imm.ASM.Util.Operands.Memory;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;

public class MemoryWordOperand extends MemoryOperand {

	public Expression value;
	
	public int intValue;
	
	public MemoryWordOperand(Expression expr) {
		this.value = expr;
	}
	
	public MemoryWordOperand(int value) {
		this.intValue = value;
	}
	
	public String toString() {
		if (this.value != null) {
			/* Use the type to string conversion to display type as number */
			String s = this.toString(value);
			return s; //((this.value instanceof Atom)? s : "\n" + s.substring(0, s.length() - 1));
		}
		else {
			/* Simple integer value */
			return ".word " + this.intValue;
		}
	}
	
	private String toString(Expression val) {
		String s = "";
		if (val instanceof Atom) {
			Atom atom = (Atom) val;
			s += ".word " + atom.getType().sourceCodeRepresentation();
		}
		else {
			return ".skip " + val.getType().wordsize() * 4;
		}
		
		return s;
	}

	public MemoryOperand clone() {
		MemoryWordOperand clone = new MemoryWordOperand(this.value);
		clone.intValue = this.intValue;
		return clone;
	}
	
}
