package Imm.AST.Expression;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class SizeOfType extends Expression {

			/* --- FIELDS --- */
	public TYPE sizeType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfType(TYPE type, Source source) {
		super(source);
		this.sizeType = type;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		System.out.println(this.pad(d + this.printDepthStep) + this.sizeType.typeString()); 
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkSizeOfType(this);
	}
	
}
