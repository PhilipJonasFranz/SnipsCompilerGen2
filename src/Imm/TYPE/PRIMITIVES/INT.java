package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class INT extends PRIMITIVE<Integer> {

	public INT() {
	
	}
	
	public INT(String initial) {
		super(initial);
		this.setValue(initial);
	}
	
	public void setValue(String value) {
		this.value = Integer.parseInt(value);
	}

	public boolean isEqual(TYPE type) {
		return type instanceof INT;
	}
	
	public String typeString() {
		return "INT" + ((this.value != null)? ": " + this.value : "");
	}
	
}
