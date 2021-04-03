package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Add extends BinaryExpression {
	
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
	
	public BinaryExpression clone() {
		return new Add(this.left.clone(), this.right.clone(), this.getSource().clone());
	}
	
} 
