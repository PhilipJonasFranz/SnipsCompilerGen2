package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public abstract class PRIMITIVE<T> extends TYPE<T> {

			/* --- CONSTRUCTORS --- */
	public PRIMITIVE() {
		super();
	}
	
	public PRIMITIVE(String initialValue) {
		this.setValue(initialValue);
	}
	
}
