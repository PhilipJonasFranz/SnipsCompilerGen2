package Imm.AST.Expression.Boolean;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

public class Compare extends Expression {

	public enum COMPARATOR {
		EQUAL, NOT_EQUAL,
		LESS_SAME, LESS_THAN, 
		GREATER_SAME, GREATER_THAN
	}
	
	public COMPARATOR comparator;
	
	public Expression left;
	
	public Expression right;
	
	public Compare(Expression leftOperand, Expression rightOperand, COMPARATOR comparator, Source source) {
		super(source);
		this.left = leftOperand;
		this.right = rightOperand;
		this.comparator = comparator;
	}
	
	public Expression left() {
		return this.left;
	}
	
	public Expression right() {
		return this.right;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Compare " + this.comparator.toString());
		this.left().print(d + this.printDepthStep, rec);
		this.right().print(d + this.printDepthStep, rec);
	}

	public List<String> buildProgram(int pad) {
		return null;
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkCompare(this);
	}
	
}
