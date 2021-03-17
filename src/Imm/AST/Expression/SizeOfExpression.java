package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class SizeOfExpression extends Expression {

			/* ---< FIELDS >--- */
	public Expression expression;
	
	public TYPE sizeType;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfExpression(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkSizeOfExpression(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
		this.expression.setContext(context);
	}

	public Expression clone() {
		return new SizeOfExpression(this.expression.clone(), this.getSource().clone());
	}

} 
