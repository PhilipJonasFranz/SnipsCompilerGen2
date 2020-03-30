package Imm.AST.Expression.Arith;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Div extends BinaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Div(Expression left, Expression right, Source source) {
		super(left, right, Operator.DIV, source);
	}

}
