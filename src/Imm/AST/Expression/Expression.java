package Imm.AST.Expression;

import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public abstract class Expression extends SyntaxElement {

	TYPE type;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Expression(Source source) {
		super(source);
	}
	
}
