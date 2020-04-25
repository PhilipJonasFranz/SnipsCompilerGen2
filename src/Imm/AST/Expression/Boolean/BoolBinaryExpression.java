package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BoolBinaryExpression extends BinaryExpression {

			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BoolBinaryExpression(Expression left, Expression right, Operator operator, Source source) {
		super(left, right, operator, source);
	}

	
			/* --- METHODS --- */
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkBoolBinaryExpression(this);
	}
	
}
