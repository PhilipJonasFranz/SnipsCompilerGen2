package Imm.AST;

import java.util.List;

import Imm.AST.Statement.Declaration;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructDeclaration extends SyntaxElement {

			/* --- FIELDS --- */
	String structName;
	
	List<Declaration> fields;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructDeclaration(Token id, List<Declaration> fields, Source source) {
		super(source);
		this.structName = id.spelling;
		this.fields = fields;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
