package Imm.AST.Expression.Arith;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class BitAnd extends BinaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BitAnd(Expression left, Expression right, Source source) {
		super(left, right, Operator.LSR, source);
	}

}
