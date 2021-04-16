package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class SizeOfExpression extends Expression {

			/* ---< FIELDS >--- */
	public Expression expression;
	
	public TYPE sizeType;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfExpression(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "SizeOf");
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkSizeOfExpression(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSizeOfExpression(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.expression.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
		this.expression.setContext(context);
	}

	public Expression clone() {
		SizeOfExpression soe = new SizeOfExpression(this.expression.clone(), this.getSource().clone());
		soe.setType(this.getType().clone());
		soe.copyDirectivesFrom(this);
		return soe;
	}

	public String codePrint() {
		return "sizeOf(" + this.expression.codePrint() + ")";
	}

} 
