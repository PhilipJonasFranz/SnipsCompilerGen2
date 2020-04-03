package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class FLOAT extends PRIMITIVE<Float> {

	public FLOAT(String value) {
		super(value);
		this.value = Float.parseFloat(value);
	}

	public boolean isEqual(TYPE type) {
		return type instanceof FLOAT;
	}
	
	public String typeString() {
		return "FLOAT";
	}

	public void setValue(String value) {
		this.value = Float.parseFloat(value);
	}

	public String sourceCodeRepresentation() {
		return "0";
	}
	
}
