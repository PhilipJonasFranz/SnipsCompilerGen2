package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class WatchStatement extends CompoundStatement {

			/* ---< FIELDS >--- */
	public Declaration watched;
	
	public boolean hasTarget = false;
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public WatchStatement(List<Statement> body, Declaration watched, Source source) {
		super(body, source);
		this.watched = watched;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Watch<" + this.watched.getType() + " " + this.watched.path + ">");
		
		if (rec) for (Statement s : this.body) 
			s.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkWatchStatement(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optWatchStatement(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.watched.visit(visitor));
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.watched.setContext(context);
		for (Statement s : this.body) 
			s.setContext(context);
	}

	public WatchStatement clone() {
		Declaration wc = null;
		if (this.watched != null) wc = this.watched.clone();
		
		WatchStatement w = new WatchStatement(this.cloneBody(), wc, this.getSource().clone());
		w.hasTarget = this.hasTarget;
		w.copyDirectivesFrom(this);
		return w;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "watch (" + this.watched.codePrint(0).get(0) + ") {";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}

} 
