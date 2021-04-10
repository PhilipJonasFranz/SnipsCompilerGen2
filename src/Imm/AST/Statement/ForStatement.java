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
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ForStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	/** 
	 * The declaration of the iterator. 
	 */
	public Declaration iterator;
	
	/** 
	 * The iterator increment statement. 
	 */
	public Statement increment;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ForStatement(Declaration iterator, Expression condition, Statement increment, List<Statement> body, Source source) {
		super(condition, body, source);
		this.iterator = iterator;
		this.increment = increment;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "For");
		
		if (rec) {
			this.iterator.print(d + this.printDepthStep, rec);
			this.condition.print(d + this.printDepthStep, rec);
			this.increment.print(d + this.printDepthStep, rec);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkForStatement(this);
		
		CompilerDriver.lastSource = temp;
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
		return new ForStatement(this.iterator.clone(), this.condition.clone(), this.increment.clone(), this.cloneBody(), this.getSource().clone());
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String inc = this.increment.codePrint(0).get(0);
		
		if (inc.endsWith(";"))
			inc = inc.substring(0, inc.length() - 1);
		
		String s = "for (" + this.iterator.codePrint(0).get(0) + " " + this.condition.codePrint() + "; " + inc + ") {";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}
	
} 
