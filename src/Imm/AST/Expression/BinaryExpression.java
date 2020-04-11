package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BinaryExpression extends Expression {

	public enum Operator {
		MUL, ADD, SUB, ORR,
		LSL, LSR, CMP, AND;
	}
	
	public Expression leftOperand;
	
	public Operator operator;
	
	public Expression rightOperand;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BinaryExpression(Expression left, Expression right, Operator operator, Source source) {
		super(source);
		this.leftOperand = left;
		this.rightOperand = right;
		this.operator = operator;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) {
			this.leftOperand.print(d + this.printDepthStep, rec);
			this.rightOperand.print(d + this.printDepthStep, rec);
		}
	}
	public Expression left() {
		return this.leftOperand;
	}
	
	public Expression right() {
		return this.rightOperand;
	}
	
	public Operator operator() {
		return this.operator;
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkBinaryExpression(this);
	}
	
}
