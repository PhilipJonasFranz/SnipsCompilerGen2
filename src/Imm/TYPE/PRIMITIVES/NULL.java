package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class NULL extends PRIMITIVE<Object> {
	
			/* ---< METHODS >--- */
	public void setValue(String value) {
		
	}
	
	public boolean isEqual(TYPE type) {
		return type.isPointer() || type.isNull();
	}
	
	public String typeString() {
		return "NULL";
	}
	
	public String sourceCodeRepresentation() {
		// TODO: Implement mechanism that replaces this with the address of the nullpointer
		return "NULL";
	}
	
	public int wordsize() {
		return 1;
	}
	
	public TYPE getCoreType() {
		return this;
	}

	public NULL clone() {
		return new NULL();
	}

} 
