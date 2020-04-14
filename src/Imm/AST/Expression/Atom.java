package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Atom extends Expression {

			/* --- FIELDS --- */
	/* Type information and potential value */
	public String spelling;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Atom(TYPE type, Token id, Source source) {
		super(source);
		this.type = type;
		this.spelling = id.spelling;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "<" + this.type.typeString() + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAtom(this);
	}
	
}
