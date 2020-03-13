package Imm.AST;

import java.util.List;

import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Program extends SyntaxElement {

			/* --- FIELDS --- */
	List<SyntaxElement> programElements;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Program(List<SyntaxElement> programElements, Source source) {
		super(source);
		this.programElements = programElements;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
