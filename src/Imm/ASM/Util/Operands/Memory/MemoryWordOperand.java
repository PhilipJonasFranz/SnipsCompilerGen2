package Imm.ASM.Util.Operands.Memory;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.Arith.UnaryMinus;
import Snips.CompilerDriver;

public class MemoryWordOperand extends MemoryOperand {

	public Expression value;
	
	public MemoryWordOperand(Expression expr) {
		this.value = expr;
	}
	
	public String toString() {
		/* Use the type to string conversion to display type as number */
		String s = this.toString(value);
		return ((this.value instanceof Atom)? s : "\n" + s.substring(0, s.length() - 1));
	}
	
	private String toString(Expression val) {
		String s = "";
		if (val instanceof Atom) {
			Atom atom = (Atom) val;
			s += ".word " + atom.type.sourceCodeRepresentation();
		}
		else {
			ArrayInit init = (ArrayInit) val;
			for (int i = 0; i < init.elements.size(); i++) {
				if (init.elements.get(i) instanceof ArrayInit) {
					s += toString((ArrayInit) init.elements.get(i));
				}
				else {
					String v = null;
					if (init.elements.get(i) instanceof UnaryMinus) {
						UnaryMinus minus = (UnaryMinus) init.elements.get(i);
						v = "-" + ((Atom) minus.getOperand()).type.sourceCodeRepresentation();
					}
					else v = ((Atom) init.elements.get(i)).type.sourceCodeRepresentation();
					s += CompilerDriver.printDepth + CompilerDriver.printDepth + ".word " + v + "\n";
				}
			}
		}
		return s;
	}
	
}
