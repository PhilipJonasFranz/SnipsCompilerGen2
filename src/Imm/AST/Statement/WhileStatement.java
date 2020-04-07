package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class WhileStatement extends ConditionalCompoundStatement {

			/* --- CONSTRUCTORS --- */
	public WhileStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		for (Directive dir : this.directives) dir.print(d, rec);
		System.out.println(this.pad(d) + "While");
		this.condition.print(d + this.printDepthStep, rec);
		
		for (Statement s : this.body) {
			s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkWhileStatement(this);
	}
	
}
