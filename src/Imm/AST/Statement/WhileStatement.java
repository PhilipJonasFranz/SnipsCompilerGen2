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
public class WhileStatement extends ConditionalCompoundStatement {

			/* ---< CONSTRUCTORS >--- */
	public WhileStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "While");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
		
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkWhileStatement(this);
	}

	public WhileStatement clone() {
		return new WhileStatement((Expression) this.condition.clone(), this.cloneBody(), this.getSource().clone());
	}
	
} 
