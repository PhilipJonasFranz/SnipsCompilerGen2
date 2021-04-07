package Imm.AST.Expression;

import java.util.List;

import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class Expression extends SyntaxElement {

			/* ---< FIELDS >--- */
	private TYPE type;
	
	
			/* ---< CONSTRUCTORS >--- */
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
	
	public abstract Expression opt(ASTOptimizer s) throws OPT0_EXC;
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
	public abstract Expression clone();
	
	public abstract String codePrint();
	
	public List<String> codePrint(int d) {
		return null;
	}
	
} 
