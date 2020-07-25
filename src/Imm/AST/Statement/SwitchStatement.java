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
public class SwitchStatement extends Statement {

			/* --- FIELDS --- */
	public Expression condition;
	
	public List<CaseStatement> cases;
	
	public DefaultStatement defaultStatement;
	
	
			/* --- CONSTRUCTORS --- */
	public SwitchStatement(Expression condition, List<CaseStatement> cases, DefaultStatement defaultStatement, Source source) {
		super(source);
		this.condition = condition;
		this.cases = cases;
		this.cases.stream().forEach(x -> x.superStatement = this);
		this.defaultStatement = defaultStatement;
		this.defaultStatement.superStatement = this;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Switch");
		this.condition.print(d + this.printDepthStep, rec);
		
		for (CaseStatement c : this.cases) {
			c.print(d + this.printDepthStep, rec);
		}
		
		this.defaultStatement.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkSwitchStatement(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.condition.setContext(context);
		for (CaseStatement c : this.cases) c.setContext(context);
		this.defaultStatement.setContext(context);
	}

}
