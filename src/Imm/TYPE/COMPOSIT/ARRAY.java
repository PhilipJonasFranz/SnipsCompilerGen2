package Imm.TYPE.COMPOSIT;

import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;

public class ARRAY extends COMPOSIT {

	public TYPE elementType;
	
	private int length;
	
	/**
	 * Create ARRAY type with length determined by constant expression.
	 */
	public ARRAY(TYPE elementType, Expression length) {
		this.elementType = elementType;
		this.length = length.getType().toInt();
	}
	
	/**
	 * Create array with given static size.
	 */
	public ARRAY(TYPE elementType, int length) {
		this.elementType = elementType;
		this.length = length;
	}
	
	public int getLength() {
		return this.length;
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

	public int wordsize() {
		return this.elementType.wordsize() * this.getLength();
	}

	public TYPE clone() {
		return new ARRAY(this.elementType.clone(), this.length);
	}
	
	public TYPE getCoreType() {
		return this.elementType.getCoreType().clone();
	}
	
	public TYPE getContainedType() {
		return this.elementType;
	}

	public TYPE provisoFree() {
		ARRAY arr = (ARRAY) this.clone();
		arr.elementType = arr.elementType.provisoFree();
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
