package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRef extends Expression {

			/* --- FIELDS --- */
	/* The name of the referenced variable */
	public String id;
	
	/* Set during context checking */
	public Declaration origin;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRef(Token id, Source source) {
		super(source);
		this.id = id.spelling;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "IDRef: " + this.id);
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkIDRef(this);
	}
	
}
