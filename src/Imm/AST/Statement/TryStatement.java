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
public class TryStatement extends CompoundStatement {

			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	public List<WatchStatement> watchpoints;
	
	public List<TYPE> unwatched = new ArrayList();
	
	public List<TYPE> actualSignals = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public TryStatement(List<Statement> body, List<WatchStatement> watchpoints, Source source) {
		super(body, source);
		this.watchpoints = watchpoints;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Try");
		
		if (rec) {
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
			
			for (WatchStatement w : this.watchpoints) 
				w.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkTryStatement(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optTryStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		for (WatchStatement w : this.watchpoints) 
			result.addAll(w.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		for (Statement s : this.body) 
			s.setContext(context);
		
		for (WatchStatement w : this.watchpoints) 
			w.setContext(context);
	}

	public TryStatement clone() {
		List<WatchStatement> watchClone = null;
		if (this.watchpoints != null) {
			watchClone = new ArrayList();
			for (WatchStatement w : this.watchpoints) watchClone.add((WatchStatement) w.clone());
		}
	
		List<TYPE> unw = new ArrayList();
		for (TYPE t : this.unwatched) unw.add(t.clone());
		
		TryStatement tr = new TryStatement(this.cloneBody(), watchClone, this.getSource().clone());
		if (this.watchpoint != null) tr.watchpoint = this.watchpoint;
		tr.copyDirectivesFrom(this);
		return tr;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "try";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		for (WatchStatement w : this.watchpoints) {
			List<String> c0 = w.codePrint(d);
			code.set(code.size() - 1, code.get(code.size() - 1) + " " + c0.get(0));
			c0.remove(0);
			code.addAll(c0);
		}
		
		return code;
	}

} 
