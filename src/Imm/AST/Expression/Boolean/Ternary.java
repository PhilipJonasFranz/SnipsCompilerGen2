package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

public class Ternary extends Expression {
	
	public Expression condition;
	
	public Expression leftOperand;
	
	public Expression rightOperand;
	
	public Ternary(Expression condition, Expression left, Expression right, Source source) {
		super(source);
		this.condition = condition;
		this.leftOperand = left;
		this.rightOperand = right;
	}
	
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Ternary");
		this.condition.print(d + this.printDepthStep, rec);
		this.leftOperand.print(d + this.printDepthStep, rec);
		this.rightOperand.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkTernary(this);
	}
	
}