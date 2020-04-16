package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ReturnStatement extends Statement {

			/* --- FIELDS --- */
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ReturnStatement(Expression value, Source source) {
		super(source);
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Return");
		if (rec) {
			if (this.value != null) this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkReturn(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		this.value.setContext(context);
	}

	public void releaseContext() {
		this.value.releaseContext();
	}
	
}
