package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Not extends BoolUnaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Not(Expression op, Source source) {
		super(op, UnaryOperator.NOT, source);
	}

}
