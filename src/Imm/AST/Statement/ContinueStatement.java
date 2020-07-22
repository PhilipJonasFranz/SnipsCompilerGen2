package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ContinueStatement extends Statement {

			/* --- FIELDS --- */
	/** The compound statement or loop this continue statements jumps in. */
	public CompoundStatement superLoop;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ContinueStatement(Source source) {
		super(source);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Continue");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkContinue(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

}
