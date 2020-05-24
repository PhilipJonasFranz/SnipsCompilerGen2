package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class RegisterAtom extends Expression {

			/* --- FIELDS --- */
	/* Type information and potential value */
	public String spelling;
	
	public REGISTER reg;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public RegisterAtom(Token id, Source source) {
		super(source);
		this.setType(new INT());
		this.spelling = id.spelling;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Register Value <" + this.getType().typeString() + " : " + this.spelling + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkRegisterAtom(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		return;
	}

	public void releaseContext() {
		return;
	}
	
}
