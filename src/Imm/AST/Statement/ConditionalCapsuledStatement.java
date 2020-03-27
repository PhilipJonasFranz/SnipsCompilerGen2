package Imm.AST.Statement;

import java.util.List;

import Imm.AST.Expression.Expression;
import Util.Source;

public abstract class ConditionalCapsuledStatement extends CapsuledStatement {

	public Expression condition;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ConditionalCapsuledStatement(Expression condition, List<Statement> body, Source source) {
		super(body, source);
		this.condition = condition;
	}

}
