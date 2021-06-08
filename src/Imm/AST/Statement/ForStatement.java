package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ForStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	/** 
	 * The declaration or IDRef of the iterator. 
	 */
	public SyntaxElement iterator;
	
	/** 
	 * The iterator increment statement. 
	 */
	public Statement increment;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ForStatement(SyntaxElement iterator, Expression condition, Statement increment, List<Statement> body, Source source) {
		super(condition, body, source);
		this.iterator = iterator;
		this.increment = increment;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "For");
		
		if (rec) {
			this.iterator.print(d + this.printDepthStep, true);
			this.condition.print(d + this.printDepthStep, true);
			this.increment.print(d + this.printDepthStep, true);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, true);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkForStatement(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optForStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.iterator.visit(visitor));
		result.addAll(this.condition.visit(visitor));
		result.addAll(this.increment.visit(visitor));
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		return result;
	}

	public Statement clone() {
		ForStatement f = new ForStatement(this.iterator.clone(), this.condition.clone(), this.increment.clone(), this.cloneBody(), this.getSource().clone());
		f.copyDirectivesFrom(this);
		return f;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String inc = this.increment.codePrint(0).get(0);
		
		if (inc.endsWith(";"))
			inc = inc.substring(0, inc.length() - 1);
		
		String it;
		/* Switch between Declaration and IDRef iterator codePrint implementations */
		if (this.iterator instanceof Declaration) it = this.iterator.codePrint(0).get(0);
		else it = ((Expression) this.iterator).codePrint() + ";";
		
		String s = "for (" + it + " " + this.condition.codePrint() + "; " + inc + ") {";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}
	
} 
