package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Util.FBin;

public class FLOAT extends PRIMITIVE<Float> {

	public FLOAT() {
	
	}
	
	public FLOAT(String initial) {
		super(initial);
		this.setValue(initial);
	}
	
	public void setValue(String value) {
		this.value = Float.parseFloat(value);
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type.isPointer()) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof FLOAT;
		}
		else return type instanceof FLOAT;
	}

	public String sourceCodeRepresentation() {
		/* Use packed int to represent float in asm */
		return "" + FBin.toDecimal(FBin.toFBin(this.value));
	}
	
	public Integer toInt() {
		return null;
	}
	
	public boolean hasInt() {
		return false;
	}
	
	public TYPE clone() {
		FLOAT b = new FLOAT();
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
	public String codeString() {
		return "float";
	}
	
} 
