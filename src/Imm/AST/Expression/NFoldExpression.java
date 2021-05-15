package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes an operation that is applied to two or more operands.
 */
public abstract class NFoldExpression extends Expression {

			/* ---< NESTED >--- */
	public enum Operator {
		MUL, ADD, SUB, ORR,
		LSL, LSR, CMP, AND,
		BOR, BAN, BXR, DIV, MOD;
		
		public String codeRepresentation() {
			if (this == MUL) return "*";
			if (this == ADD) return "+";
			if (this == SUB) return "-";
			if (this == ORR) return "||";
			if (this == LSL) return "<<";
			if (this == LSR) return ">>";
			if (this == AND) return "&&";
			if (this == BOR) return "|";
			if (this == BAN) return "&";
			if (this == BXR) return "^";
			
			new Message("Cannot translate operator " + this + " to string!", Type.WARN);
			
			return "?";
		}
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
				e.print(d + this.printDepthStep, true);
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
