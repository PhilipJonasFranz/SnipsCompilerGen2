package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.UnaryExpression;
import Opt.AST.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class BitNot extends UnaryExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BitNot(Expression operand, Source source) {
		super(operand, UnaryOperator.NOT, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optBitNot(this);
	}
	
	public UnaryExpression clone() {
		BitNot not = new BitNot(this.getOperand().clone(), this.getSource().clone());
		
		if (this.getType() != null)
			not.setType(this.getType().clone());
		
		not.copyDirectivesFrom(this);
		return not;
	}
	
	public String codePrint() {
		return "~" + this.operand.codePrint();
	}

} 
