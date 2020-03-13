package Imm.AST.Statement;

import java.util.List;

import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

			/* --- FIELDS --- */
	String fieldName;
		
	TYPE type;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(Token id, TYPE type, Source source) {
		super(source);
		this.fieldName = id.spelling;
		this.type = type;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
