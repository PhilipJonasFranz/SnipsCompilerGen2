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
public class IfStatement extends CapsuledStatement {

			/* --- FIELDS --- */
	public Expression condition;
	
	public IfStatement elseStatement;
	
			/* --- CONSTRUCTORS --- */
	public IfStatement(Expression condition, List<Statement> body, Source source) {
		super(body, source);
		this.condition = condition;
	}
	
	public IfStatement(List<Statement> body, Source source) {
		super(body, source);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "If");
		this.condition.print(d + this.printDepthStep, rec);
		
		for (Statement s : this.body) {
			s.print(d + this.printDepthStep, rec);
		}
		
		IfStatement if0 = this.elseStatement;
		while (if0 != null) {
			if (if0.condition != null) {
				System.out.println(this.pad(d) + "Else If");
				if0.condition.print(d + this.printDepthStep, rec);
				
				for (Statement s : if0.body) {
					s.print(d + this.printDepthStep, rec);
				}
				
				if0 = if0.elseStatement;
			}
			else {
				System.out.println(this.pad(d) + "Else");
				
				for (Statement s : if0.body) {
					s.print(d + this.printDepthStep, rec);
				}
				
				return;
			}
		}
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkIfStatement(this);
	}
	
}
