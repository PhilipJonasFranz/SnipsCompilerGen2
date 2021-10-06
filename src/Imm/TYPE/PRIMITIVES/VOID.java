package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class VOID extends PRIMITIVE<Void> {

	public VOID() {
	
	}
	
	public void setValue(String value) {
		this.value = null;
	}

	public boolean isEqual(TYPE type) {
		/* Acts as dont care */
		return true;
	}
	
	public String typeString() {
		return "VOID";
	}

	public String sourceCodeRepresentation() {
		return null;
	}
	
	public TYPE clone() {
		VOID b = new VOID();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
	public String codeString() {
		return "void";
	}
	
} 
