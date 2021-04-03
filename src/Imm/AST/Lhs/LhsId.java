package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public abstract class LhsId extends SyntaxElement {

			/* ---< FIELDS >--- */
	/* Set during context checking */
	public Declaration origin;
	
	public TYPE expressionType;
	
	public Assignment assign;
	
	
			/* ---< CONSTRUCTORS >--- */
	public LhsId(Source source) {
		super(source);
	}
	
	
			/* ---< METHODS >--- */
	public abstract void print(int d, boolean rec);

	/**
	 * Implementations provide their own check implementations, since they relay to 
	 * internal components and have to set {@link #origin} after checking.
	 */
	public abstract TYPE check(ContextChecker ctx) throws CTEX_EXC;
	
	public abstract LhsId opt(ASTOptimizer s) throws OPT0_EXC;
	
	public abstract NamespacePath getFieldName();
	
	public abstract LhsId clone();
	
} 
