package Imm.AST.Expression;

import Imm.AST.SyntaxElement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class Expression extends SyntaxElement {

			/* --- FIELDS --- */
	private TYPE type;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Expression(Source source) {
		super(source);
	}
	
	/** 
	 * Return the current context, or the actual type.
	 */
	public TYPE getType() {
		if (this.type instanceof PROVISO) {
			PROVISO p = (PROVISO) this.type;
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.type;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
} 
