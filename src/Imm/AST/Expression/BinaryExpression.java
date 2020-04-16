package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BinaryExpression extends Expression {

			/* --- NESTED --- */
	public enum Operator {
		MUL, ADD, SUB, ORR,
		LSL, LSR, CMP, AND;
	}
	
	
			/* --- FIELDS --- */
	/** The left operand expression */
	@Getter
	public Expression left;
	
	/** The operator */
	@Getter
	public Operator operator;
	
	/** The right operand expression */
	@Getter
	public Expression right;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BinaryExpression(Expression left, Expression right, Operator operator, Source source) {
		super(source);
		this.left = left;
		this.right = right;
		this.operator = operator;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) {
			this.left.print(d + this.printDepthStep, rec);
			this.right.print(d + this.printDepthStep, rec);
		}
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkBinaryExpression(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		this.left.setContext(context);
		this.right.setContext(context);
	}

	public void releaseContext() {
		this.left.releaseContext();
		this.right.releaseContext();
	}
	
}
