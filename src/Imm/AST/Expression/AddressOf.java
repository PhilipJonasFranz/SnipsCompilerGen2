package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class AddressOf extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public AddressOf(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "AddressOf");
		this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAddressOf(this);
	}
	
}
