package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDOfExpression extends Expression {

			/* ---< FIELDS >--- */
	public TYPE type;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDOfExpression(TYPE type, Source source) {
		super(source);
		this.type = type;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "IDOf " + this.type);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIDOfExpression(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optIDOfExpression(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.type, context);
	}

	public Expression clone() {
		IDOfExpression iof = new IDOfExpression(this.type.clone(), this.getSource().clone());
		iof.setType(this.getType().clone());
		iof.copyDirectivesFrom(this);
		return iof;
	}

	public String codePrint() {
		return "idof(" + this.type.codeString() + ")";
	}

} 
