package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
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
public class BreakStatement extends Statement {

			/* ---< FIELDS >--- */
	/** The loop this break statements breaks out of. */
	public CompoundStatement superLoop;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BreakStatement(Source source) {
		super(source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Break");
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkBreak(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) {
		return opt.optBreakStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}
	
	public void setContext(List<TYPE> context) {}

	public Statement clone() {
		BreakStatement b = new BreakStatement(this.getSource().clone());
		b.superLoop = this.superLoop;
		b.copyDirectivesFrom(this);
		return b;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		code.add(Util.pad(d) + "break;");
		return code;
	}

} 
