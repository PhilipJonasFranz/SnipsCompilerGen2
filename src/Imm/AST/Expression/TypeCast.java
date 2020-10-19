package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTX_EXC;
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
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkTypeCast(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		/** Apply context to cast type */
		ProvisoUtil.mapNTo1(this.castType, context);
		
		this.expression.setContext(context);
	}

} 
