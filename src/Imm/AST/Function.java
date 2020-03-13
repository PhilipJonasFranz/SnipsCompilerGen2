package Imm.AST;

import java.util.List;

import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends SyntaxElement {

			/* --- FIELDS --- */
	String functionName;
	
	List<Declaration> parameters;
	
	List<Statement> statements;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Function(Token functionId, List<Declaration> parameters, List<Statement> statements, Source source) {
		super(source);
		this.functionName = functionId.spelling;
		this.parameters = parameters;
		this.statements = statements;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
