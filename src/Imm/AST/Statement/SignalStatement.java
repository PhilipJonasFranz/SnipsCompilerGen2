package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SignalStatement extends Statement {

			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	public Expression exceptionBuilder;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SignalStatement(Expression shadowRef, Source source) {
		super(source);
		this.exceptionBuilder = shadowRef;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Signal");
		if (rec) this.exceptionBuilder.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkSignal(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSignalStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.exceptionBuilder.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.exceptionBuilder != null) 
			this.exceptionBuilder.setContext(context);
	}

	public Statement clone() {
		SignalStatement s = new SignalStatement(this.exceptionBuilder.clone(), this.getSource().clone());
		if (this.watchpoint != null) 
			s.watchpoint = this.watchpoint;
		
		s.copyDirectivesFrom(this);
		return s;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		code.add(Util.pad(d) + "signal " + this.exceptionBuilder.codePrint() + ";");
		return code;
	}

} 
