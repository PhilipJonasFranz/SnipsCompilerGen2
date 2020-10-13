package Imm.AST.Expression;

import java.util.List;

import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class SizeOfExpression extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	public TYPE sizeType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfExpression(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		this.expression.print(d + this.printDepthStep, rec);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
		
		this.expression.setContext(context);
	}

} 
