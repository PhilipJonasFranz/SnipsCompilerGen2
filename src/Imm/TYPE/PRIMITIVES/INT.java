package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;

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
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type.isPointer()) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof INT;
		}
		else return type instanceof INT;
	}
	
	public String typeString() {
		return "INT";
	}

	public String sourceCodeRepresentation() {
		return "" + this.value;
	}
	
	public Integer toInt() {
		return this.value;
	}
	
	public TYPE clone() {
		INT b = new INT();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
	public String codeString() {
		return "int";
	}
	
} 
