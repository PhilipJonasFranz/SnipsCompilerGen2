package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class InstanceofExpression extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	public TYPE instanceType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InstanceofExpression(Expression expression, TYPE instanceType, Source source) {
		super(source);
		this.expression = expression;
		this.instanceType = instanceType;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "InstanceOf");
		
		if (rec) {
			this.expression.print(d + this.printDepthStep, rec);
			System.out.println(this.pad(d + this.printDepthStep) + this.instanceType.typeString());
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkInstanceofExpression(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (this.instanceType instanceof PROVISO) 
			ProvisoUtil.mapNTo1(this.instanceType, context);
			
		this.expression.setContext(context);
	}

} 
