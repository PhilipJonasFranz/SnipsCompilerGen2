package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		CompilerDriver.outs.println(Util.pad(d) + "TypeCast");
		CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + this.castType);
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkTypeCast(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optTypeCast(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.expression.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		/** Apply context to cast type */
		ProvisoUtil.mapNTo1(this.castType, context);
		
		this.expression.setContext(context);
	}

	public Expression clone() {
		TypeCast tc = new TypeCast(this.expression.clone(), this.castType.clone(), this.getSource().clone());
		tc.setType(this.getType().clone());
		tc.copyDirectivesFrom(this);
		return tc;
	}

	public String codePrint() {
		return "(" + this.castType.codeString() + ") " + this.expression.codePrint();
	}
	
	/**
	 * Returns true if this type cast is trivial. A trivial cast is a cast that, during AsN casting,
	 * does not have to generate any additional assembly instructions. There are only two cases where
	 * this is needed: <br>
	 *  <br>
	 * 		FLOAT -> Non-FLOAT <br>
	 * 		Non-FLOAT -> FLOAT <br>
	 *  <br>
	 * In any other case the cast is trivial.
	 */
	public boolean isTrivialCast() {
		return !((this.expression.getType().isFloat() && !this.castType.isFloat()) || 
				(!this.expression.getType().isFloat() && this.castType.isFloat()));
	}

} 
