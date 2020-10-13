package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Deref extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Deref(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Deref");
		this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkDeref(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.expression.setContext(context);
	}

} 
