package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
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
		//System.out.println("Applied Context: " + this.getClass().getName());
		
		if (this.castType instanceof PROVISO) {
			PROVISO pro = (PROVISO) this.castType;
			boolean found = false;
			for (int i = 0; i < context.size(); i++) {
				if (context.get(i).isEqual(pro)) {
					pro.setContext(context.get(i));
					found = true;
					break;
				}
			}
			
			if (!found) {
				throw new CTX_EXCEPTION(this.getSource(), "Unknown proviso " + pro.typeString());
			}
		}
		
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
