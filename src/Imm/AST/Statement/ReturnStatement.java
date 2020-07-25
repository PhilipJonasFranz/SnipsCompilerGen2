package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
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
			if (this.value != null) 
				this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkReturn(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (this.value != null) 
			this.value.setContext(context);
	}

}
