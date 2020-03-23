package Imm.AST.Statement;

import java.util.List;

import Util.Source;

public abstract class CapsuledStatement extends Statement {

	public List<Statement> body;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public CapsuledStatement(List<Statement> body, Source source) {
		super(source);
		this.body = body;
	}

}
