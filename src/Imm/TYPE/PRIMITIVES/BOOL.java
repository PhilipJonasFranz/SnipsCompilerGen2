package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;

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
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type.isPointer()) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof BOOL;
		}
		return type instanceof BOOL;
	}
	
	public String typeString() {
		return "BOOL";
	}

	public String sourceCodeRepresentation() {
		return (this.value)? "1" : "0";
	}
	
	public Integer toInt() {
		return (this.value)? 1 : 0;
	}

	public TYPE clone() {
		BOOL b = new BOOL();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
} 
