package Imm.TYPE;

import Exc.SNIPS_EXC;
import Snips.CompilerDriver;

public class PROVISO extends TYPE<Void> {

			/* --- FIELDS --- */
	public String placeholderName;
	
	protected TYPE context;
	
	
			/* --- CONSTRUCTORS --- */
	public PROVISO(String placeholderName) {
		this.placeholderName = placeholderName;
	}

	
			/* --- METHODS --- */
	public void setContext(TYPE type) {
		if (type instanceof PROVISO) {
			while (type instanceof PROVISO) {
				type = ((PROVISO) type).getContext();
			}
		}
		
		this.context = type;
	}
	
	public TYPE getContext() {
		if (this.context instanceof PROVISO) {
			PROVISO p = (PROVISO) this.context;
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.context;
	}
	
	public boolean hasContext() {
		return this.context != null;
	}
	
	public void releaseContext() {
		this.context = null;
	}
	
	public void setValue(String value) {
		if (this.context != null) this.context.setValue(value);
	}
	
	public boolean isEqual(TYPE type) {
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			return p.placeholderName.equals(this.placeholderName);
		}
		else {
			if (this.context == null) return false;
			else return this.context.isEqual(type);
		}
	}
	
	public boolean hasValue() {
		return (this.context != null)? this.context.hasValue() : false;
	}
	
	public String typeString() {
		String s = "";
		if (this.context == null || CompilerDriver.includeProvisoInTypeString) {
			s += "PROVISO<";
			s += this.placeholderName;
			if (this.context != null) s += ", " + this.context.typeString();
			s += ">";
			
			if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
			
			return s;
		}
		else {
			s = this.context.typeString();
			if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
			return s;
		}
	}
	
	public String sourceCodeRepresentation() {
		return (this.context != null)? this.context.sourceCodeRepresentation() : null;
	}
	
	public int wordsize() {
		if (this.context != null) return this.context.wordsize();
		else {
			throw new SNIPS_EXC("INTERNAL : Attempted to get word size of PROVISO " + this.placeholderName + " without context!");
		}
	}
	
	public TYPE getCoreType() {
		return (this.hasContext())? this.context.getCoreType() : this;
	}

	public PROVISO clone() {
		PROVISO p = new PROVISO(this.placeholderName);
		if (this.context != null) p.context = this.context.clone();
		return p;
	}

	public TYPE provisoFree() {
		if (this.hasContext())
			return this.getContext().clone();
		else throw new SNIPS_EXC("Cannot free contextless proviso: " + this.placeholderName);
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		if (this.placeholderName.equals(name)) 
			return newType;
		else return this;
	}

}
