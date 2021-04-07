package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Sub extends BinaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Sub(Expression left, Expression right, Source source) {
		super(left, right, Operator.SUB, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSub(this);
	}
	
	public BinaryExpression clone() {
		Sub e = new Sub(this.left.clone(), this.right.clone(), this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		return this.left.codePrint() + " - " + this.right.codePrint();
	}

} 
