package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ArrayInit extends Expression {

			/* --- FIELDS --- */
	public List<Expression> elements;
	
	public boolean dontCareTypes = false;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ArrayInit(List<Expression> elements, boolean dontCare, Source source) {
		super(source);
		this.elements = elements;
		this.dontCareTypes = dontCare;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ArrayInit " + ((this.getType() != null)? this.getType().typeString() : "?"));
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkArrayInit(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
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
