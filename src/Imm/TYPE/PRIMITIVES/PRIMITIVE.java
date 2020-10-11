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
	
	public int wordsize() {
		return 1;
	}
	
	public TYPE getCoreType() {
		return this;
	}
	
	public TYPE provisoFree() {
		return this.clone();
	}
	
	public TYPE remapProvisoName(String name, TYPE newType) {
		return this;
	}
	
	public TYPE mappable(TYPE mapType, String searchedProviso) {
		return null;
	}
	
	public boolean hasProviso() {
		return false;
	}
	
} 
