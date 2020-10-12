package Imm.TYPE.COMPOSIT;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;

public class ARRAY extends COMPOSIT {

	public TYPE elementType;
	
	private Expression length0;
	
	private TYPE coreType;
	
	public int length;
	
	public ARRAY(TYPE elementType, Expression length) {
		this.elementType = elementType;
		if (elementType instanceof PRIMITIVE) {
			this.coreType = elementType;
		}
		else {
			this.coreType = elementType.getCoreType();
		}
		this.length0 = length;
	}
	
	public ARRAY(TYPE elementType, int length) {
		this.elementType = elementType;
		if (elementType instanceof PRIMITIVE) {
			this.coreType = elementType;
		}
		else {
			this.coreType = elementType.getCoreType();
		}
		this.length = length;
		this.wordSize = elementType.wordsize() * length;
	}
	
	public int getLength() {
		if (this.length0 == null) return this.length;
		else {
			this.length = ((INT) ((Atom) this.length0).getType()).value;
			this.length0 = null;
			this.wordSize = this.elementType.wordsize() * this.length;
			return this.length;
		}
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			if (p.hasContext()) return this.isEqual(p.getContext());
			else return false;
		}
		if (type instanceof ARRAY) {
			ARRAY array = (ARRAY) type;
			return this.elementType.isEqual(array.elementType) && this.getLength() == array.getLength();
		}
		else if (type instanceof POINTER) {
			return this.getCoreType().isEqual(type.getCoreType());
		}
		else return false;
	}

	public String typeString() {
		String s = this.coreType.typeString().split(":") [0] + "[" + this.getLength() + "]";
		TYPE t = this.elementType;
		while (t instanceof ARRAY) {
			s += "[" + ((ARRAY) t).getLength() + "]";
			t = ((ARRAY) t).elementType;
		}
		return s;
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
			this.length = ((INT) ((Atom) this.length0).getType()).value;
			this.wordSize = this.elementType.wordsize() * this.length;
		}
		
		return this.wordSize;
	}

	public TYPE clone() {
		if (this.length0 != null) {
			ARRAY arr = new ARRAY(this.elementType.clone(), this.length0);
			return arr;
		}
		else return new ARRAY(this.elementType.clone(), this.length);
	}
	
	public TYPE getCoreType() {
		return this.coreType;
	}

	public TYPE provisoFree() {
		ARRAY arr = (ARRAY) this.clone();
		arr.elementType = arr.elementType.provisoFree();
		arr.coreType = arr.coreType.provisoFree();
		return arr;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		this.elementType = this.elementType.remapProvisoName(name, newType);
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType instanceof ARRAY) {
			ARRAY arr = (ARRAY) mapType;
			return this.elementType.mappable(arr.elementType, searchedProviso);
		}
		else return null;
	}

	public boolean hasProviso() {
		return this.elementType.hasProviso();
	}

} 
