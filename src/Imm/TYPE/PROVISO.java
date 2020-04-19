package Imm.TYPE;

import Exc.SNIPS_EXCEPTION;

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
		return this.context;
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
		if (this.context == null)
			return true;
			//return type instanceof PROVISO && ((PROVISO) type).placeholderName.equals(this.placeholderName);
		else {
			if (type instanceof PROVISO) {
				PROVISO p = (PROVISO) type;
				return p.placeholderName.equals(this.placeholderName);
			}
			else {
				return this.context.isEqual(type);
			}
		}
	}
	
	public boolean hasValue() {
		return (this.context != null)? this.context.hasValue() : false;
	}
	
	public String typeString() {
		return "PROVISO<" + ((this.context != null)? this.placeholderName + ", " + this.context.typeString() : this.placeholderName) + ">";
	}
	
	public String sourceCodeRepresentation() {
		return (this.context != null)? this.context.sourceCodeRepresentation() : null;
	}
	
	public int wordsize() {
		if (this.context != null) return this.context.wordsize();
		else {
			try {
				throw new SNIPS_EXCEPTION("Attempted to get word size of PROVISO " + this.placeholderName + " without context!");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	}
	
	public TYPE getCoreType() {
		return (this.context != null)? this.context.getCoreType() : this;
	}

	public TYPE clone() {
		PROVISO p = new PROVISO(this.placeholderName);
		if (this.context != null) p.context = this.context.clone();
		return p;
	}

}
