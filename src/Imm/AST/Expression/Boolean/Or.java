package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.Expression;
import Util.Source;

public class Or extends BoolBinaryExpression {

			/* --- CONSTRUCTORS --- */
	public Or(Expression left, Expression right, Source source) {
		super(left, right, Operator.ORR, source);
	}
	
}
