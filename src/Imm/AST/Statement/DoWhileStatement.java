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
public class DoWhileStatement extends ConditionalCompoundStatement {

			/* ---< CONSTRUCTORS >--- */
	public DoWhileStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Do While");
		
		if (rec) {
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		
			this.condition.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkDoWhileStatement(this);
		
		ctx.popTrace();
		return t;
	}

	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDoWhileStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.condition.visit(visitor));
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		return result;
	}
	
	public Statement clone() {
		DoWhileStatement dw = new DoWhileStatement(this.condition.clone(), this.cloneBody(), this.getSource().clone());
		dw.copyDirectivesFrom(this);
		return dw;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "do {";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "} while (" + this.condition.codePrint() + ");");
		return code;
	}
	
} 
