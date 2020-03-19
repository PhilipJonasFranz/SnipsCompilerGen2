package Imm.TYPE.COMPOSIT;

import Imm.TYPE.TYPE;

public class ARRAY extends TYPE {

	TYPE elementType;
	
	public ARRAY(String value, TYPE elementType) {
		super(value);
		this.elementType = elementType;
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof ARRAY) {
			ARRAY array = (ARRAY) type;
			return this.elementType.isEqual(array.elementType);
		}
		else return false;
	}

	public String typeString() {
		return this.elementType.typeString() + "[]";
	}
	
}
