package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class TypeCast extends Expression {

			/* ---< FIELDS >--- */
	public Expression expression;
	
	public TYPE castType;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public TypeCast(Expression expression, TYPE castType, Source source) {
		super(source);
		this.expression = expression;
		this.castType = castType;
	}
	

			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "TypeCast");
		System.out.println(this.pad(d + this.printDepthStep) + this.castType.typeString());
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkTypeCast(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optTypeCast(this);
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		/** Apply context to cast type */
		ProvisoUtil.mapNTo1(this.castType, context);
		
		this.expression.setContext(context);
	}

	public Expression clone() {
		return new TypeCast(this.expression.clone(), this.castType.clone(), this.getSource().clone());
	}

} 
