package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
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
	
	private Declaration(String id, TYPE type, Source source) {
		super(source);
		this.fieldName = id;
		this.type = type;
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
		/* Apply to declaration type */
		TYPE type0 = this.type;
		boolean pointer = false;
		if (type0 instanceof POINTER) {
			type0 = ((POINTER) type0).targetType;
			pointer = true;
		}
		
		if (type0 instanceof PROVISO) {
			PROVISO p = (PROVISO) type0;
			for (int i = 0; i < context.size(); i++) {
				TYPE pro = context.get(i);
				if (pro.isEqual(p)) {
					p.setContext(context.get(i));
				}
			}
		}
		else if (type0 instanceof STRUCT) {
			STRUCT s = (STRUCT) type0;
			if (!pointer) s.typedef.setContext(context);
			
			s.proviso = context;
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
	
	public TYPE getRawType() {
		return this.type;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
	public Declaration clone() {
		Declaration clone = new Declaration(this.fieldName, this.type.clone(), this.getSource());
		return clone;
	}
	
}
