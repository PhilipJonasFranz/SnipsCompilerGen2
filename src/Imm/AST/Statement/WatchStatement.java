package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class WatchStatement extends CompoundStatement {

			/* --- FIELDS --- */
	public Declaration watched;
	
	public boolean hasTarget = false;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public WatchStatement(List<Statement> body, Declaration watched, Source source) {
		super(body, source);
		this.watched = watched;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Watch<" + this.watched.getType().typeString() + " " + this.watched.path.build() + ">");
		if (rec) {
			for (Statement s : this.body) {
				s.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkWatchStatement(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		this.watched.setContext(context);
		for (Statement s : this.body) {
			s.setContext(context);
		}
	}

	public void releaseContext() {
		this.watched.releaseContext();
		for (Statement s : this.body) {
			s.releaseContext();
		}
	}
	
}