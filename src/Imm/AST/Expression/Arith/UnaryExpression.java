package Imm.AST.Expression.Arith;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class UnaryExpression extends Expression {

	public enum UnaryOperator {
		NEG, MIN
	}
	
	public UnaryOperator operator;
	
	public Expression operand;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public UnaryExpression(Expression operand, UnaryOperator operator, Source source) {
		super(source);
		this.operand = operand;
		this.operator = operator;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) {
			this.operand.print(d + this.printDepthStep, rec);
		}
	}
	public Expression operand() {
		return this.operand;
	}
	
	public UnaryOperator operator() {
		return this.operator;
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkUnaryExpression(this);
	}
	
}
