package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class IfStatement extends Statement {

			/* --- FIELDS --- */
	public Expression condition;
	
	public List<Statement> body;
	
	public IfStatement elseStatement;
	
			/* --- CONSTRUCTORS --- */
	public IfStatement(Expression condition, List<Statement> body, Source source) {
		super(source);
		this.condition = condition;
		this.body = body;
	}
	
	public IfStatement(List<Statement> body, Source source) {
		super(source);
		this.body = body;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		if (this.condition != null) {
			System.out.println(this.pad(d) + "If");
			this.condition.print(d + this.printDepthStep, rec);
		}
		else {
			System.out.println(this.pad(d) + "Else");
		}
		
		for (Statement s : this.body) {
			s.print(d + this.printDepthStep, rec);
		}
		
		if (this.elseStatement != null) {
			this.elseStatement.print(d, rec);
		}
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkIfStatement(this);
	}
	
}
