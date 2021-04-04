package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.StructureInit;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SignalStatement extends Statement {

			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	private Expression shadowRef;
	
	public StructureInit exceptionInit;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SignalStatement(Expression shadowRef, Source source) {
		super(source);
		this.shadowRef = shadowRef;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Signal");
		if (rec) this.shadowRef.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		if (this.shadowRef instanceof StructureInit) {
			this.exceptionInit = (StructureInit) this.shadowRef;
		}
		else throw new CTEX_EXC(this.getSource(), "Expected structure init, but got " + this.shadowRef.getClass().getName());
		
		TYPE t = ctx.checkSignal(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSignalStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.shadowRef.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.shadowRef != null) 
			this.shadowRef.setContext(context);
	}

	public Statement clone() {
		SignalStatement s = new SignalStatement(this.shadowRef.clone(), this.getSource().clone());
		if (this.watchpoint != null) 
			s.watchpoint = this.watchpoint;
		
		if (this.exceptionInit != null)
			s.exceptionInit = this.exceptionInit;
		
		return s;
	}

} 
