package Imm.AST.Statement;

import java.util.List;

import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

public abstract class CompoundStatement extends Statement {

			/* --- FIELDS --- */
	public List<Statement> body;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public CompoundStatement(List<Statement> body, Source source) {
		super(source);
		this.body = body;
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (Statement s : this.body) {
			s.setContext(context);
		}
	}

	public void releaseContext() {
		for (Statement s : this.body) {
			s.releaseContext();
		}
	}

}
