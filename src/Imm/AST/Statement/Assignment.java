package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Assignment extends Statement {

			/* --- FIELDS --- */
	public String fieldName;
	
	public Declaration origin;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	public Assignment(Token fieldName, Expression value, Source source) {
		super(source);
		this.fieldName = fieldName.spelling;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign " + this.fieldName);
		if (rec) {
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAssignment(this);
	}
	
}
