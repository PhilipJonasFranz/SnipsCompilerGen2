package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class IfStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	public IfStatement elseStatement;
	
	
			/* ---< CONSTRUCTORS >--- */
	public IfStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
		this.condition = condition;
	}
	
	public IfStatement(List<Statement> body, Source source) {
		super(null, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "If");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
		
		IfStatement if0 = this.elseStatement;
		while (if0 != null) {
			if (if0.condition != null) {
				System.out.println(this.pad(d) + "Else If");
				
				if (rec) {
					if0.condition.print(d + this.printDepthStep, rec);
					
					for (Statement s : if0.body) 
						s.print(d + this.printDepthStep, rec);
				}
				
				if0 = if0.elseStatement;
			}
			else {
				System.out.println(this.pad(d) + "Else");
				
				if (rec) for (Statement s : if0.body) 
					s.print(d + this.printDepthStep, rec);
				
				return;
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIfStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optIfStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		if (this.condition != null) result.addAll(this.condition.visit(visitor));
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		if (this.elseStatement != null)
			result.addAll(this.elseStatement.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		super.setContext(context);
		
		if (this.condition != null) 
			this.condition.setContext(context);
		
		if (this.elseStatement != null) 
			this.elseStatement.setContext(context);
	}

	public Statement clone() {
		IfStatement i = new IfStatement(this.condition.clone(), this.cloneBody(), this.getSource().clone());
		if (this.elseStatement != null)
			i.elseStatement = (IfStatement) this.elseStatement.clone();
		return i;
	}

} 
