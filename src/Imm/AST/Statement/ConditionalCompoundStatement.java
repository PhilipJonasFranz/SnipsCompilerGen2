package Imm.AST.Statement;

import java.util.List;

import Exc.CTEX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

public abstract class ConditionalCompoundStatement extends CompoundStatement {

			/* ---< FIELDS >--- */
	/** The condition that has to be true so that the body is executed, f.E if-statement */
	public Expression condition;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ConditionalCompoundStatement(Expression condition, List<Statement> body, Source source) {
		super(body, source);
		this.condition = condition;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		super.setContext(context);
		if (this.condition != null) 
			this.condition.setContext(context);
	}

} 
