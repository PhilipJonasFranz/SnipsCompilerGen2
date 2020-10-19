package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ForStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	/** The declaration of the iterator. */
	public Declaration iterator;
	
	/** The iterator increment statement. */
	public Statement increment;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ForStatement(Declaration iterator, Expression condition, Statement increment, List<Statement> body, Source source) {
		super(condition, body, source);
		this.iterator = iterator;
		this.increment = increment;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "For");
		
		if (rec) {
			this.iterator.print(d + this.printDepthStep, rec);
			this.condition.print(d + this.printDepthStep, rec);
			this.increment.print(d + this.printDepthStep, rec);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkForStatement(this);
	}
	
} 
