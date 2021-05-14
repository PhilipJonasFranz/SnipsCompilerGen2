package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * A compound statement capsules a list of statements.
 */
public abstract class CompoundStatement extends Statement {

			/* ---< FIELDS >--- */
	/** All statements contained in the body of the compound statement. */
	public List<Statement> body;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public CompoundStatement(List<Statement> body, Source source) {
		super(source);
		this.body = body;
	}
	
	
			/* ---< METHODS >--- */
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		for (Statement s : this.body) 
			s.setContext(context);
	}
	
	public List<Statement> cloneBody() {
		List<Statement> clone = new ArrayList();
		for (Statement s : this.body) clone.add(s.clone());
		return clone;
	}

} 
