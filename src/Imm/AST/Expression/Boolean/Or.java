package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Util.Source;

public class Or extends BoolBinaryExpression {

			/* ---< CONSTRUCTORS >--- */
	public Or(Expression left, Expression right, Source source) {
		super(left, right, Operator.ORR, source);
	}

	public BinaryExpression clone() {
		return new Or(this.left.clone(), this.right.clone(), this.getSource().clone());
	}
} 
