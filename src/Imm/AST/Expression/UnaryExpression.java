package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class UnaryExpression extends Expression {

			/* ---< NESTED >--- */
	public enum UnaryOperator {
		NOT, NEG;
	}
	
	
			/* ---< FIELDS >--- */
	/** The operator */
	private UnaryOperator operator;
	
	/** The expression operand */
	private Expression operand;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public UnaryExpression(Expression operand, UnaryOperator operator, Source source) {
		super(source);
		this.operand = operand;
		this.operator = operator;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) this.operand.print(d + this.printDepthStep, rec);
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkUnaryExpression(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.operand.setContext(context);
	}

	public Expression getOperand() {
		return this.operand;
	}
	
	public UnaryOperator getOperator() {
		return this.operator;
	}
	
} 
