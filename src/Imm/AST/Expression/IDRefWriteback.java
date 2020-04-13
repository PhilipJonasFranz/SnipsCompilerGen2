package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRefWriteback extends Expression {

			/* --- NESTED --- */
	public enum ID_WRITEBACK {
		INCR, DECR;
	}
	
	
			/* --- FIELDS --- */
	public ID_WRITEBACK idWb;
	
	@Getter
	private Expression shadowRef;
	
	public IDRef idRef;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRefWriteback(ID_WRITEBACK idWb, Expression expression, Source source) {
		super(source);
		this.idWb = idWb;
		this.shadowRef = expression;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Increment");
		this.shadowRef.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkIDRefWriteback(this);
	}
	
}
