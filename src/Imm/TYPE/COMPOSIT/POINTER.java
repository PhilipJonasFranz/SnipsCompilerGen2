package Imm.TYPE.COMPOSIT;

import Imm.TYPE.TYPE;

public class POINTER extends TYPE {

	TYPE targetType;
	
	public POINTER(TYPE targetType) {
		this.targetType = targetType;
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof POINTER) {
			POINTER pointer = (POINTER) type;
			return this.targetType.isEqual(pointer.targetType);
		}
		else return false;
	}
	
}
