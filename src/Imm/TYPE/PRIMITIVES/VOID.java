package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class VOID extends PRIMITIVE<Void> {

	public VOID() {
	
	}

	public void setValue(String value) {}

	public boolean isEqual(TYPE type) {
		/* Acts as dont care */
		return true;
	}
	
	public String sourceCodeRepresentation() {
		return null;
	}
	
	public TYPE clone() {
		return new VOID();
	}
	
	public String codeString() {
		return "void";
	}
	
} 
