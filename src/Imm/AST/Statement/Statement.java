package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.SyntaxElement;
import Util.Source;

/**
 * This class represents a superclass for all Statements.
 */
public abstract class Statement extends SyntaxElement {

			/* ---< FIELDS >--- */
	/**
	 * Contains all declarations that can be freed after this
	 * statement was excecuted. See 'AsNStatement.freeDecs()'.
	 */
	public List<Declaration> free = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Statement(Source source) {
		super(source);
	}

	public abstract Statement clone();
	
} 
