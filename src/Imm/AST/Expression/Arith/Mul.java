package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Mul extends BinaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Mul(Expression left, Expression right, Source source) {
		super(left, right, Operator.MUL, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optMul(this);
	}
	
	public BinaryExpression clone() {
		return new Mul(this.left.clone(), this.right.clone(), this.getSource().clone());
	}

} 
