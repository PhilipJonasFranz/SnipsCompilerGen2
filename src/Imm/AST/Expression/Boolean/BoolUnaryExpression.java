package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.UnaryExpression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BoolUnaryExpression extends UnaryExpression {

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BoolUnaryExpression(Expression operand, UnaryOperator operator, Source source) {
		super(operand, operator, source);
	}

	
			/* ---< METHODS >--- */
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkBoolUnaryExpression(this);
		
		ctx.popTrace();
		return t;
	}
	
} 
