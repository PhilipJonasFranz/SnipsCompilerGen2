package Imm.AST.Statement;

import java.util.List;

import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

public abstract class ConditionalCompoundStatement extends CompoundStatement {

			/* --- FIELDS --- */
	public Expression condition;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ConditionalCompoundStatement(Expression condition, List<Statement> body, Source source) {
		super(body, source);
		this.condition = condition;
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		super.setContext(context);
		this.condition.setContext(context);
	}

	public void releaseContext() {
		super.releaseContext();
		this.condition.releaseContext();
	}

}
