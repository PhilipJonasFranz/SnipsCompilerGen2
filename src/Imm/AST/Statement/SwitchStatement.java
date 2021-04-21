package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SwitchStatement extends Statement {

			/* ---< FIELDS >--- */
	public Expression condition;
	
	public List<CaseStatement> cases;
	
	public DefaultStatement defaultStatement;
	
	
			/* ---< CONSTRUCTORS >--- */
	public SwitchStatement(Expression condition, List<CaseStatement> cases, DefaultStatement defaultStatement, Source source) {
		super(source);
		this.condition = condition;
		this.cases = cases;
		this.cases.stream().forEach(x -> x.superStatement = this);
		this.defaultStatement = defaultStatement;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Switch");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
			
			for (CaseStatement c : this.cases) 
				c.print(d + this.printDepthStep, rec);
			
			this.defaultStatement.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkSwitchStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSwitchStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.condition.visit(visitor));
		for (CaseStatement s : this.cases)
			result.addAll(s.visit(visitor));
		result.addAll(this.defaultStatement.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.condition.setContext(context);
		for (CaseStatement c : this.cases) c.setContext(context);
		this.defaultStatement.setContext(context);
	}

	public SwitchStatement clone() {
		List<CaseStatement> casesC = new ArrayList();
		for (CaseStatement w : this.cases) casesC.add((CaseStatement) w.clone());
		
		SwitchStatement s = new SwitchStatement((Expression) this.condition.clone(), casesC, (DefaultStatement) this.defaultStatement.clone(), this.getSource().clone());
		s.copyDirectivesFrom(this);
		return s;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "switch (" + this.condition.codePrint() + ") {";
		code.add(Util.pad(d) + s);
		
		for (CaseStatement s0 : this.cases)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.addAll(this.defaultStatement.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}

} 
