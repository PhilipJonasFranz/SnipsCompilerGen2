package Imm.TYPE.COMPOSIT;

import Imm.TYPE.TYPE;

public class POINTER extends TYPE {

	TYPE targetType;
	
	public POINTER(String value, TYPE targetType) {
		super(value);
		this.targetType = targetType;
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof POINTER) {
			POINTER pointer = (POINTER) type;
			return this.targetType.isEqual(pointer.targetType);
		}
		else return false;
	}
	
	public String typeString() {
		return "*" + this.targetType.typeString();
	}

	@Override
	public void setValue(String value) {
		return;
	}

	@Override
	public String sourceCodeRepresentation() {
		return null;
	}

	@Override
	public int wordsize() {
		return 1;
	}
	
}
