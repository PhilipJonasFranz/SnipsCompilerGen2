package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class UnaryExpression extends Expression {

			/* ---< NESTED >--- */
	public enum UnaryOperator {
		NOT, NEG
	}
	
	
			/* ---< FIELDS >--- */
	/** The operator */
	private UnaryOperator operator;
	
	/** The expression operand */
	public Expression operand;
	
	
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
		CompilerDriver.outs.println(Util.pad(d) + this.operator.toString());
		if (rec) this.operand.print(d + this.printDepthStep, true);
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkUnaryExpression(this);
		
		ctx.popTrace();
		return t;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.operand.visit(visitor));

		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.operand.setContext(context);
	}

	public Expression getOperand() {
		return this.operand;
	}
	
	public UnaryOperator getOperator() {
		return this.operator;
	}
	
} 
