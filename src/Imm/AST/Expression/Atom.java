package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
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
	
	public boolean isPlaceholder = false;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Atom(TYPE type, Token id, Source source) {
		super(source);
		this.setType(type);
		this.spelling = id.spelling;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Atom <" + this.getType().typeString() + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkAtom(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		//System.out.println("Applied Context: " + this.getClass().getName());
		return;
	}

} 
