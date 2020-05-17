package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.StructureInit;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SignalStatement extends Statement {

			/* --- FIELDS --- */
	private Expression shadowRef;
	
	public StructureInit exceptionInit;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SignalStatement(Expression shadowRef, Source source) {
		super(source);
		this.shadowRef = shadowRef;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Signal");
		if (rec) {
			this.shadowRef.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		if (this.shadowRef instanceof StructureInit) {
			this.exceptionInit = (StructureInit) this.shadowRef;
		}
		else throw new CTX_EXCEPTION(this.getSource(), "Expected structure init, but got " + this.shadowRef.getClass().getName());
		return ctx.checkSignal(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		if (this.shadowRef != null) this.shadowRef.setContext(context);
	}

	public void releaseContext() {
		this.shadowRef.releaseContext();
	}
	
}
