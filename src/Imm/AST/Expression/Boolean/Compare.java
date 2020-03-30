package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.TYPE.TYPE;
import Util.Source;

public class Compare extends BinaryExpression {

	public enum COMPARATOR {
		EQUAL, NOT_EQUAL,
		LESS_SAME, LESS_THAN, 
		GREATER_SAME, GREATER_THAN
	}
	
	public COMPARATOR comparator;
	
	public Compare(Expression left, Expression right, COMPARATOR comparator, Source source) {
		super(left, right, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Compare " + this.comparator.toString());
		this.left().print(d + this.printDepthStep, rec);
		this.right().print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkCompare(this);
	}
	
}
