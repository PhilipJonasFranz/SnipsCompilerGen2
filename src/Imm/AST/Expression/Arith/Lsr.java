package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Lsr extends BinaryExpression {
	
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
	
	public BinaryExpression clone() {
		return new Lsr(this.left.clone(), this.right.clone(), this.getSource().clone());
	}
	
	public String codePrint() {
		return this.left.codePrint() + " >> " + this.right.codePrint();
	}
	
} 
