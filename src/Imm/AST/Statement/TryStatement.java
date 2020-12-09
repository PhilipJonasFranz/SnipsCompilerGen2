package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class TryStatement extends CompoundStatement {

			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	public List<WatchStatement> watchpoints;
	
	public List<TYPE> unwatched = new ArrayList();
	
	
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
		System.out.println(this.pad(d) + "Try");
		
		if (rec) {
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
			
			for (WatchStatement w : this.watchpoints) 
				w.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkTryStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
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
		return tr;
	}

} 
