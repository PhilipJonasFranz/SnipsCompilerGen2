package Imm.AST.Expression.Boolean;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Opt.AST.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Not extends BoolUnaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Not(Expression op, Source source) {
		super(op, UnaryOperator.NOT, source);
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optNot(this);
	}
	
	public BoolUnaryExpression clone() {
		Not not = new Not(this.getOperand().clone(), this.getSource().clone());
		not.setType(this.getType().clone());
		not.copyDirectivesFrom(this);
		return not;
	}
	
	public String codePrint() {
		return "!" + this.operand.codePrint();
	}
	
} 
