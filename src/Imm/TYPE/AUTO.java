package Imm.TYPE;

import Exc.SNIPS_EXC;

public class AUTO extends TYPE<Void> {

			/* ---< CONSTRUCTORS >--- */
	public AUTO() {
		
	}

	public void setValue(String value) {
		throw new SNIPS_EXC("Cannot apply value to type AUTO!");
	}

	public boolean isEqual(TYPE type) {
		return type instanceof AUTO;
	}

	public String typeString() {
		return "AUTO";
	}

	public String codeString() {
		return "auto";
	}

	public String sourceCodeRepresentation() {
		throw new SNIPS_EXC("Cannot get Source-Code Representation of type AUTO!");
	}

	public int wordsize() {
		throw new SNIPS_EXC("Cannot get Wordsize of type AUTO!");
	}

	public TYPE getCoreType() {
		return this;
	}

	public TYPE getContainedType() {
		return this;
	}

	public TYPE clone() {
		return new AUTO();
	}

	public TYPE provisoFree() {
		return this;
	}

	public boolean hasProviso() {
		return false;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		return null;
	}

} 
