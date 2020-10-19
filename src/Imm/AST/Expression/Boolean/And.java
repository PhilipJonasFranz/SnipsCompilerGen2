package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.Expression;
import Util.Source;

public class And extends BoolBinaryExpression {

			/* ---< CONSTRUCTORS >--- */
	public And(Expression left, Expression right, Source source) {
		super(left, right, Operator.AND, source);
	}

} 
