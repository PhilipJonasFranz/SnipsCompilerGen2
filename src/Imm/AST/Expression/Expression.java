package Imm.AST.Expression;

import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Util.Source;

import java.util.List;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class Expression extends SyntaxElement {

			/* ---< FIELDS >--- */
	protected TYPE type;
	
	public String operatorSymbolOverride;
	
	
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
		if (this.type instanceof PROVISO p) {
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.type;
	}
	
	public abstract Expression opt(ASTOptimizer s) throws OPT0_EXC;
	
	public void setType(TYPE type) {
		this.type = type.clone();
	}
	
	public abstract Expression clone();
	
	public abstract String codePrint();
	
	public List<String> codePrint(int d) {
		return null;
	}

}
