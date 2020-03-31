package Imm.AST.Expression.Boolean;

import Imm.AST.Expression.Expression;
import Imm.AST.Expression.Arith.BinaryExpression;
import Util.Source;

public class Or extends BinaryExpression {

	public Or(Expression left, Expression right, Source source) {
		super(left, right, Operator.ORR, source);
	}
	
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Or");
		this.left().print(d + this.printDepthStep, rec);
		this.right().print(d + this.printDepthStep, rec);
	}
	
}
