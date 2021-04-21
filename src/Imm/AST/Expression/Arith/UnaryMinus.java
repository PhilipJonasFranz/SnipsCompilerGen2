package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.UnaryExpression;
import Opt.AST.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class UnaryMinus extends UnaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public UnaryMinus(Expression operand, Source source) {
		super(operand, UnaryOperator.NEG, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optUnaryMinus(this);
	}
	
	public UnaryExpression clone() {
		UnaryMinus um = new UnaryMinus(this.getOperand().clone(), this.getSource().clone());
		um.setType(this.getType().clone());
		um.copyDirectivesFrom(this);
		return um;
	}
	
	public String codePrint() {
		return "-" + this.operand.codePrint();
	}

} 
