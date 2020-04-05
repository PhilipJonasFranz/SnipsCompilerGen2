package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Assignment extends Statement {

			/* --- FIELDS --- */
	public Expression target;
	
	public Declaration origin;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	public Assignment(Expression target, Expression value, Source source) {
		super(source);
		this.target = target;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign");
		this.target.print(d + this.printDepthStep, rec);
		if (rec) {
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAssignment(this);
	}
	
}
