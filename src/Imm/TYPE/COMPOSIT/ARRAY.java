package Imm.TYPE.COMPOSIT;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

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
	}
	
	public int getLength() {
		if (this.length0 == null) return this.length;
		else {
			this.length = ((INT) ((Atom) this.length0).getType()).value;
			this.length0 = null;
			return this.length;
		}
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			if (p.hasContext()) return this.isEqual(p.getContext());
			else return false;
		}
		if (type.isArray()) {
			ARRAY array = (ARRAY) type;
			return this.elementType.isEqual(array.elementType) && this.getLength() == array.getLength();
		}
		else if (type.isPointer()) {
			return this.getCoreType().isEqual(type.getCoreType());
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

	public int wordsize() {
		return this.elementType.wordsize() * this.getLength();
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
	
	public TYPE getContainedType() {
		return this.elementType;
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
		if (mapType.isArray()) {
			ARRAY arr = (ARRAY) mapType;
			return this.elementType.mappable(arr.elementType, searchedProviso);
		}
		else return null;
	}

	public boolean hasProviso() {
		return this.elementType.hasProviso();
	}
	
	public String codeString() {
		return this.elementType.codeString() + " [" + this.getLength() + "]";
	}

} 
