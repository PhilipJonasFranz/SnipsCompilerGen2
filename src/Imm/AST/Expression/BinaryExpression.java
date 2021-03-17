package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class BinaryExpression extends Expression {

			/* ---< NESTED >--- */
	public enum Operator {
		MUL, ADD, SUB, ORR,
		LSL, LSR, CMP, AND;
	}
	
	
			/* ---< FIELDS >--- */
	/** The left operand expression */
	public Expression left;
	
	/** The operator */
	public Operator operator;
	
	/** The right operand expression */
	public Expression right;
	
	
			/* ---< CONSTRUCTORS >--- */
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

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + this.operator.toString());
		if (rec) {
			this.left.print(d + this.printDepthStep, rec);
			this.right.print(d + this.printDepthStep, rec);
		}
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkBinaryExpression(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.left.setContext(context);
		this.right.setContext(context);
	}
	
	public Expression getLeft() {
		return this.left;
	}
	
	public Expression getRight() {
		return this.right;
	}
	
	public Operator getOperator() {
		return this.operator;
	}
	
} 
