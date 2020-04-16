package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

			/* --- FIELDS --- */
	public String fieldName;
		
	private TYPE type;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(Token id, TYPE type, Source source) {
		super(source);
		this.fieldName = id.spelling;
		this.type = type;
	}
	
	public Declaration(Token id, TYPE type, Expression value, Source source) {
		super(source);
		this.fieldName = id.spelling;
		this.type = type;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Declaration <" + this.type.typeString() + "> " + this.fieldName);
		if (rec && this.value != null) {
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkDeclaration(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		
		/* Apply to declaration type */
		if (this.type instanceof PROVISO) {
			PROVISO p = (PROVISO) this.type;
			for (int i = 0; i < context.size(); i++) {
				TYPE pro = context.get(i);
				
				if (pro.isEqual(p)) {
					p.setContext(context.get(i));
				}
			}
		}
		
		/* Apply to value */
		if (this.value != null) this.value.setContext(context);
	}

	public void releaseContext() {
		if (this.value != null) this.value.releaseContext();
	}
	
	/** 
	 * Return the current context, or the actual type.
	 */
	public TYPE getType() {
		if (this.type instanceof PROVISO) {
			PROVISO p = (PROVISO) this.type;
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.type;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
}
