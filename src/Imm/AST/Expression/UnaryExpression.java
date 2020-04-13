package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class UnaryExpression extends Expression {

			/* --- NESTED --- */
	public enum UnaryOperator {
		NOT, NEG;
	}
	
	
			/* --- FIELDS --- */
	/** The operator */
	@Getter
	private UnaryOperator operator;
	
	/** The expression operand */
	@Getter
	private Expression operand;
	
	
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

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) {
			this.operand.print(d + this.printDepthStep, rec);
		}
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkUnaryExpression(this);
	}
	
}
