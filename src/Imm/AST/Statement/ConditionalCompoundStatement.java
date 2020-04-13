package Imm.AST.Statement;

import java.util.List;

import Imm.AST.Expression.Expression;
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

}
