package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Atom extends Expression {

	/* Type information and potential value */
	public TYPE type;
	
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

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "<" + this.type.typeString() + ">");
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAtom(this);
	}
	
}
