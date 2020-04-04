package Imm.TYPE.COMPOSIT;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;

public class ARRAY extends TYPE {

	public TYPE elementType;
	
	private Expression length0;
	
	public int length;
	
	public ARRAY(TYPE elementType, Expression length) {
		super(null);
		this.elementType = elementType;
		this.length0 = length;
		this.wordSize = elementType.wordsize();
	}
	
	public ARRAY(TYPE elementType, int length) {
		super(null);
		this.elementType = elementType;
		this.length = length;
		this.wordSize = elementType.wordsize() * length;
	}
	
	public int getLength() {
		if (this.length0 == null) return this.length;
		else {
			this.length = ((INT) ((Atom) this.length0).type).value;
			this.wordSize = this.elementType.wordsize() * this.length;
			return this.length;
		}
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof ARRAY) {
			ARRAY array = (ARRAY) type;
			return this.elementType.isEqual(array.elementType) && this.getLength() == array.getLength();
		}
		else return false;
	}

	public String typeString() {
		return this.elementType.typeString() + "[" + this.getLength() + "]";
	}

	public void setValue(String value) {
		/* No value for arrays */
		return;
	}

	public String sourceCodeRepresentation() {
		return null;
	}

	@Override
	public int wordsize() {
		if (this.length0 != null) {
			this.length = ((INT) ((Atom) this.length0).type).value;
			this.wordSize = this.elementType.wordsize() * this.length;
		}
		return this.wordSize;
	}
	
}
