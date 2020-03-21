package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class BOOL extends TYPE<Boolean> {

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
		return "BOOL";
	}
	
}
