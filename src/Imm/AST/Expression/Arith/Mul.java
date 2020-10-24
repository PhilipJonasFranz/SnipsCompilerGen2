package Imm.AST.Expression.Arith;

import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
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
	
	public BinaryExpression clone() {
		return new Mul(this.left.clone(), this.right.clone(), this.getSource().clone());
	}

} 
