package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class BOOL extends PRIMITIVE<Boolean> {

	public BOOL() {
	
	}
	
	public BOOL(String initial) {
		super(initial);
		this.setValue(initial);
	}
	
	public void setValue(String value) {
		this.value = Boolean.parseBoolean(value);
	}

	public boolean isEqual(TYPE type) {
		return type instanceof BOOL;
	}
	
	public String typeString() {
		return "BOOL" + ((this.value != null)? ": " + this.value : "");
	}

	public String sourceCodeRepresentation() {
		return (this.value)? "1" : "0";
	}

	public TYPE clone() {
		BOOL b = new BOOL();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
}
