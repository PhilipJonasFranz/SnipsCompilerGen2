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
public class CaseStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	public SwitchStatement superStatement;
	
	
			/* ---< CONSTRUCTORS >--- */
	public CaseStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Case");
		this.condition.print(d + this.printDepthStep, rec);
		
		if (rec) for (Statement s : this.body) 
			s.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkCaseStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public Statement clone() {
		CaseStatement c = new CaseStatement(this.condition.clone(), this.cloneBody(), this.getSource().clone());
		c.superStatement = this.superStatement;
		return c;
	}
	
} 
