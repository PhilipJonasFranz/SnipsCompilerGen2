package Imm.AST.Statement;

import Imm.AST.SyntaxElement;
import Util.Source;

/**
 * This class represents a superclass for all Statements.
 */
public abstract class Statement extends SyntaxElement {

			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Statement(Source source) {
		super(source);
	}

}
