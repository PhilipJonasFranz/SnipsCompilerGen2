package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public class BOOL extends TYPE {

	public BOOL() {
		
	}

	public boolean isEqual(TYPE type) {
		return type instanceof BOOL;
	}
	
}
