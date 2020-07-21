package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXC;
import Imm.AST.Expression.Expression;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

			/* --- FIELDS --- */
	/** The visibility modifer of this declaration. Can only be applied to global declarations. */
	public MODIFIER modifier;
	
	/** The namespace path of this declaration. */
	public NamespacePath path;
	
	/** The effective type of this declaration. */
	private TYPE type;
	
	/** The initial value of this declaration. */
	public Expression value;
	
	public Statement last = null;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(NamespacePath path, TYPE type, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		this.type = type;
		this.modifier = modifier;
	}
	
	public Declaration(NamespacePath path, TYPE type, Expression value, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		this.type = type;
		this.value = value;
		this.modifier = modifier;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		try {
			System.out.println(this.pad(d) + "Declaration <" + this.type.typeString() + "> " + this.path.build());
		} catch (Exception e) {
			System.out.println(this.pad(d) + "Declaration <?> " + this.path.build());
		}
		
		if (rec && this.value != null) 
			this.value.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkDeclaration(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
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
		Declaration clone = new Declaration(this.path, this.type.clone(), this.modifier, this.getSource());
		return clone;
	}
	
}
