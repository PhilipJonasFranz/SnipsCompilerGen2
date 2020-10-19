package Imm.AST.Expression.Arith;

import Imm.AST.Expression.Expression;
import Imm.AST.Expression.UnaryExpression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class UnaryMinus extends UnaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public UnaryMinus(Expression operand, Source source) {
		super(operand, UnaryOperator.NEG, source);
	}

} 
