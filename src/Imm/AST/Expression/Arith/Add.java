package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Add extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Add(Expression left, Expression right, Source source) {
		super(left, right, Operator.ADD, source);
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAdd(this);
	}
	
	public Add clone() {
		Add e = new Add(this.left.clone(), this.right.clone(), this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		return this.left.codePrint() + " + " + this.right.codePrint();
	}
	
} 
