package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.Expression;
import Util.Source;

public class And extends BoolBinaryExpression {

	public And(Expression left, Expression right, Source source) {
		super(left, right, Operator.AND, source);
	}
	
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "And");
		this.left().print(d + this.printDepthStep, rec);
		this.right().print(d + this.printDepthStep, rec);
	}

}
