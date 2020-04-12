package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public class Decrement extends Expression {

			/* --- FIELDS --- */
	@Getter
	private Expression shadowRef;
	
	public IDRef idRef;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Decrement(Expression expression, Source source) {
		super(source);
		this.shadowRef = expression;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Decrement");
		this.shadowRef.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkDecrement(this);
	}
	
}
