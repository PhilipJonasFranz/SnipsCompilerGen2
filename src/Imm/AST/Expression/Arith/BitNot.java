package Imm.AST.Expression.Arith;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class BitNot extends UnaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BitNot(Expression operand, Source source) {
		super(operand, UnaryOperator.NOT, source);
	}

}
