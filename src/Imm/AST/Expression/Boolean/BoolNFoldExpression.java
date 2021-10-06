package Imm.AST.Expression.Boolean;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BoolNFoldExpression extends NFoldExpression {

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BoolNFoldExpression(Expression left, Expression right, Operator operator, Source source) {
		super(left, right, operator, source);
	}
	
	public BoolNFoldExpression(List<Expression> operands, Operator operator, Source source) {
		super(operands, operator, source);
	}

	
			/* ---< METHODS >--- */
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkBoolNFoldExpression(this);
		
		ctx.popTrace();
		return t;
	}
	
} 
