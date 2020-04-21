package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ArrayInit extends Expression {

			/* --- FIELDS --- */
	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ArrayInit(List<Expression> elements, Source source) {
		super(source);
		this.elements = elements;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ArrayInit " + ((this.getType() != null)? this.getType().typeString() : "?"));
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkArrayInit(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (Expression e : this.elements) {
			e.setContext(context);
		}
	}

	public void releaseContext() {
		for (Expression e : this.elements) {
			e.releaseContext();
		}
	}
	
}
