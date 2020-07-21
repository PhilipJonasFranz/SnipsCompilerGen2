package Imm.TYPE;

import Exc.SNIPS_EXC;
import Imm.TYPE.COMPOSIT.POINTER;

public class NULL extends TYPE<Object> {
	
			/* --- METHODS --- */
	public void setValue(String value) {
		
	}
	
	public boolean isEqual(TYPE type) {
		return type instanceof POINTER || type instanceof NULL;
	}
	
	public String typeString() {
		return "NULL";
	}
	
	public String sourceCodeRepresentation() {
		throw new SNIPS_EXC("Cannot get Source Code representation of null type.");
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
