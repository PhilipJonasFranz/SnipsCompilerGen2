package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;

public abstract class PRIMITIVE<T> extends TYPE<T> {

			/* ---< CONSTRUCTORS >--- */
	public PRIMITIVE() {
		super();
	}
	
	public PRIMITIVE(String initialValue) {
		this.setValue(initialValue);
	}
	
	/**
	 * Sets the value of this type. Types extending from this type use different parsing
	 * methods to convert the given string to a parameterized value format.
	 * @param value The value to set to this type.
	 */
	public abstract void setValue(String value);
	
	/**
	 * Returns the value of this type in a form that can be written into the generated assembly
	 * file. For example, an int would just return the stored number, a char would return the UTF-8 value
	 * of the stored character.
	 */
	public abstract String sourceCodeRepresentation();
	
	public int wordsize() {
		return 1;
	}
	
	public TYPE getCoreType() {
		return this;
	}
	
	public TYPE getContainedType() {
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
