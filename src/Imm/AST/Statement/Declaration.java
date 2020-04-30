package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	private TYPE type;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(NamespacePath path, TYPE type, Source source) {
		super(source);
		this.path = path;
		this.type = type;
	}
	
	public Declaration(NamespacePath path, TYPE type, Expression value, Source source) {
		super(source);
		this.path = path;
		this.type = type;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		try {
			System.out.println(this.pad(d) + "Declaration <" + this.type.typeString() + "> " + this.path.build());
		} catch (Exception e) {
			System.out.println(this.pad(d) + "Declaration <?> " + this.path.build());
		}
		if (rec && this.value != null) {
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkDeclaration(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		/* Apply to declaration type */
		ProvisoManager.setContext(context, this.type);
		
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
	
	public TYPE getRawType() {
		return this.type;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
	public Declaration clone() {
		Declaration clone = new Declaration(this.path, this.type.clone(), this.getSource());
		return clone;
	}
	
}
