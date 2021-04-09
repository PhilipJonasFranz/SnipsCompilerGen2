package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Lsr extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Lsr(Expression left, Expression right, Source source) {
		super(left, right, Operator.LSR, source);
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optLsr(this);
	}
	
	public NFoldExpression clone() {
		Lsr e = new Lsr(this.left.clone(), this.right.clone(), this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		return this.left.codePrint() + " >> " + this.right.codePrint();
	}
	
} 
