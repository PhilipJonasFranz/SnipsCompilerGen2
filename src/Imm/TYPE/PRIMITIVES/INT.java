package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.PROVISO;
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
		if (type instanceof VOID) return true;
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else return type instanceof INT;
	}
	
	public String typeString() {
		return "INT" + ((this.value != null)? ": " + this.value : "");
	}

	public String sourceCodeRepresentation() {
		return "" + this.value;
	}
	
	public TYPE clone() {
		INT b = new INT();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
}
