package Imm.AST.Expression;

import java.util.List;

import Exc.CTX_EXC;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRefWriteback extends Expression {
	
			/* --- FIELDS --- */
	public WRITEBACK writeback;
	
	private Expression shadowRef;
	
	public IDRef idRef;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRefWriteback(WRITEBACK idWb, Expression expression, Source source) {
		super(source);
		this.writeback = idWb;
		this.shadowRef = expression;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Increment");
		this.shadowRef.print(d + this.printDepthStep, rec);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.shadowRef.setContext(context);
	}

	public Expression getShadowRef() {
		return this.shadowRef;
	}
	
} 
