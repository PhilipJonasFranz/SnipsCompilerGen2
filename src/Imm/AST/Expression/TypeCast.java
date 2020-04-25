package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class TypeCast extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	public TYPE castType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public TypeCast(Expression expression, TYPE castType, Source source) {
		super(source);
		this.expression = expression;
		this.castType = castType;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "TypeCast");
		System.out.println(this.pad(d + this.printDepthStep) + this.castType.typeString());
		this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkTypeCast(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		/** Apply context to cast type */
		ProvisoManager.setContext(context, this.castType);
		
		this.expression.setContext(context);
	}

	public void releaseContext() {
		if (this.castType instanceof PROVISO) {
			PROVISO pro = (PROVISO) this.castType;
			pro.releaseContext();
		}
		this.expression.releaseContext();
	}
	
}
