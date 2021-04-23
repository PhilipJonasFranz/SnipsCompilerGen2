package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * Describes an operation that is applied to two or more operands.
 */
public abstract class NFoldExpression extends Expression {

			/* ---< NESTED >--- */
	public enum Operator {
		MUL, ADD, SUB, ORR,
		LSL, LSR, CMP, AND,
		BOR, BAN, BXR;
	}
	
	
			/* ---< FIELDS >--- */
	/** The operands of the expression, from left to right. */
	public List<Expression> operands = new ArrayList();
	
	/** The operator */
	public Operator operator;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public NFoldExpression(Expression left, Expression right, Operator operator, Source source) {
		super(source);
		
		this.operands.add(left);
		this.operands.add(right);
		
		this.operator = operator;
	}
	
	public NFoldExpression(List<Expression> operands, Operator operator, Source source) {
		super(source);
		
		this.operands = operands;
		this.operator = operator;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + this.operator.toString());
		if (rec) {
			for (Expression e : this.operands)
				e.print(d + this.printDepthStep, rec);
		}
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkNFoldExpression(this);
		
		ctx.popTrace();
		return t;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Expression e : this.operands)
			result.addAll(e.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		for (Expression e : this.operands)
			e.setContext(context);
	}
	
	public Operator getOperator() {
		return this.operator;
	}
	
} 
