package Imm.AST.Statement;

import java.util.List;

import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

public abstract class CompoundStatement extends Statement {

			/* --- FIELDS --- */
	/** All statements contained in the body of the compound statement. */
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
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		for (Statement s : this.body) 
			s.setContext(context);
	}

	public void releaseContext() {
		for (Statement s : this.body) 
			s.releaseContext();
	}

}
