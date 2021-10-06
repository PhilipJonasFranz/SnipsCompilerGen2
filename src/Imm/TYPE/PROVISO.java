package Imm.TYPE;

import Ctx.ContextChecker;
import Exc.SNIPS_EXC;
import Res.Const;
import Snips.CompilerDriver;

public class PROVISO extends TYPE<Void> {

	public static boolean COMPARE_NAMES = true;
	
			/* ---< FIELDS >--- */
	public String placeholderName;
	
	protected TYPE context;
	
	public TYPE defaultContext = null;
	
	
			/* ---< CONSTRUCTORS >--- */
	public PROVISO(String placeholderName) {
		this.placeholderName = placeholderName;
	}

	
			/* ---< METHODS >--- */
	public void setContext(TYPE type) {
		if (type != null && type.isProviso()) {
			while (type != null && type.isProviso()) {
				type = ((PROVISO) type).getContext();
			}
		}
		
		this.context = type;
	}
	
	public TYPE getContext() {
		if (this.context != null) {
			if (this.context.isProviso()) {
				PROVISO p = (PROVISO) this.context;
				if (p.hasContext()) return p.getContext();
				else return p;
			}
			else return this.context;
		}
		else return this.defaultContext;
	}
	
	public boolean hasContext() {
		return this.context != null || this.defaultContext != null;
	}
	
	public void releaseContext() {
		this.context = null;
	}
	
	public boolean isEqual(TYPE type) {
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			if (!PROVISO.COMPARE_NAMES) return true;
			else return p.placeholderName.equals(this.placeholderName);
		}
		else {
			if (this.context == null) {
				if (this.defaultContext != null)
					return this.defaultContext.isEqual(type);
				else return false;
			}
			else return this.context.isEqual(type);
		}
	}
	
	public String typeString() {
		String s = "";
		
		s += "PROVISO<";
		s += this.placeholderName;
		if (this.context != null) s += ", " + this.context.typeString();
		if (this.defaultContext != null) s += " : " + this.defaultContext.typeString();
		s += ">";
			
		if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
			
		return s;
	}
	
	public int wordsize() {
		if (this.context != null) return this.context.wordsize();
		else if (this.defaultContext != null) return this.defaultContext.wordsize();
		else {
			ContextChecker.progress.abort();
			throw new SNIPS_EXC(Const.ATTEMPTED_TO_GET_WORDSIZE_OF_PROVISO_WITHOUT_CONTEXT, this.placeholderName);
		}
	}
	
	public TYPE getCoreType() {
		return (this.context != null)? this.context.getCoreType() : ((this.defaultContext != null)? this.defaultContext : this);
	}
	
	public TYPE getContainedType() {
		return this;
	}

	public PROVISO clone() {
		PROVISO p = new PROVISO(this.placeholderName);
		if (this.context != null) p.context = this.context.clone();
		if (this.defaultContext != null) p.defaultContext = this.defaultContext.clone();
		return p;
	}

	public TYPE provisoFree() {
		if (this.hasContext())
			return this.getContext().clone().provisoFree();
		else {
			if (ContextChecker.progress != null) ContextChecker.progress.abort();
			throw new SNIPS_EXC(Const.CANNOT_FREE_CONTEXTLESS_PROVISO, this.placeholderName, CompilerDriver.stackTrace.peek().getSource().getSourceMarker());
		}
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		if (this.placeholderName.equals(name)) 
			return newType;
		else return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		/* Base case, map type maps directley to proviso */
		return (searchedProviso.equals(this.placeholderName))? mapType : null;
	}
	
	public Integer toInt() {
		return (this.hasContext())? this.getContext().toInt() : null;
	}

	public boolean hasProviso() {
		return true;
	}

	public String codeString() {
		String s = this.placeholderName;
		if (this.defaultContext != null)
			s += ":" + this.defaultContext.codeString();
		return s;
	}

} 
