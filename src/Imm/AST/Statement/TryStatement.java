package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class TryStatement extends CompoundStatement {

			/* --- FIELDS --- */
	public SyntaxElement watchpoint;
	
	public List<WatchStatement> watchpoints;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public TryStatement(List<Statement> body, List<WatchStatement> watchpoints, Source source) {
		super(body, source);
		this.watchpoints = watchpoints;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Try");
		if (rec) {
			for (Statement s : this.body) {
				s.print(d + this.printDepthStep, rec);
			}
			for (WatchStatement w : this.watchpoints) {
				w.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkTryStatement(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (Statement s : this.body) {
			s.setContext(context);
		}
		for (WatchStatement w : this.watchpoints) {
			w.setContext(context);
		}
	}

	public void releaseContext() {
		for (Statement s : this.body) {
			s.releaseContext();
		}
		for (WatchStatement w : this.watchpoints) {
			w.releaseContext();
		}
	}
	
}
