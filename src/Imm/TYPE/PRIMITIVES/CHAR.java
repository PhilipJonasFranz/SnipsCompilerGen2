package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;

public class CHAR extends PRIMITIVE<Character> {

	public CHAR() {
	
	}
	
	public CHAR(String initial) {
		super(initial);
		this.setValue(initial);
	}
	
	public void setValue(String value) {
		/* Termination char */
		if (value == null) 
			this.value = (char) 0;
		else this.value = value.charAt(0);
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof CHAR;
		}
		else return type instanceof CHAR;
	}
	
	public String typeString() {
		return "CHAR";
	}

	public String sourceCodeRepresentation() {
		return "" + (int) this.value;
	}
	
	public TYPE clone() {
		CHAR b = new CHAR();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
} 
