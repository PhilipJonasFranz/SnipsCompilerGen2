package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Atom extends Expression {

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Atom(TYPE type, Source source) {
		super(source);
		this.setType(type);
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Atom <" + this.getType().typeString() + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkAtom(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		
	}

	public Expression clone() {
		return new Atom(this.getType().clone(), this.getSource().clone());
	}

} 
