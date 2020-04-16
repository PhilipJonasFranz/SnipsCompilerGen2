package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class VOID extends PRIMITIVE<Void> {

	public VOID() {
	
	}
	
	public VOID(String initial) {
		super(initial);
		this.setValue(initial);
	}
	
	public void setValue(String value) {
		this.value = null;
	}

	public boolean isEqual(TYPE type) {
		return type instanceof VOID;
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
	
}
