package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class CHAR extends PRIMITIVE<Character> {

	public CHAR(String value) {
		super(value);
		this.setValue(value);
	}

	public boolean isEqual(TYPE type) {
		return type instanceof CHAR;
	}
	
	public String typeString() {
		return "CHAR";
	}

	public void setValue(String value) {
		this.value = value.charAt(0);
	}

	public String sourceCodeRepresentation() {
		return (int) this.value;
	}
	
}
