package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ContinueStatement extends Statement {

			/* ---< FIELDS >--- */
	/** The compound statement or loop this continue statements jumps in. */
	public CompoundStatement superLoop;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ContinueStatement(Source source) {
		super(source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Continue");
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkContinue(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optContinueStatement(this);
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		return;
	}

	public Statement clone() {
		ContinueStatement b = new ContinueStatement(this.getSource().clone());
		b.superLoop = this.superLoop;
		return b;
	}
	
} 
