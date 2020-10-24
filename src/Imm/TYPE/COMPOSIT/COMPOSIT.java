package Imm.TYPE.COMPOSIT;

import Imm.TYPE.TYPE;

public abstract class COMPOSIT<T> extends TYPE<T> {

			/* ---< CONSTRUCTORS >--- */
	public COMPOSIT() {
		super();
	}
	
	public COMPOSIT(String initialValue) {
		this.setValue(initialValue);
	}
	
	public int wordsize() {
		return 1;
	}
	
} 
