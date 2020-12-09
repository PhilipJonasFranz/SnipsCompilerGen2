package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class DoWhileStatement extends ConditionalCompoundStatement {

			/* ---< CONSTRUCTORS >--- */
	public DoWhileStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Do While");
		
		if (rec) {
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		
			this.condition.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkDoWhileStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public Statement clone() {
		return new DoWhileStatement(this.condition.clone(), this.cloneBody(), this.getSource().clone());
	}
	
} 
