package Imm.AST.Expression.Arith;

import java.util.ArrayList;
import java.util.List;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Opt.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Mul extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Mul(Expression left, Expression right, Source source) {
		super(left, right, Operator.MUL, source);
	}
	
	public Mul(List<Expression> operands, Source source) {
		super(operands, Operator.MUL, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optMul(this);
	}
	
	public Mul clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Mul e = new Mul(op0, this.getSource().clone());
		
		if (this.getType() != null)
			e.setType(this.getType().clone());
		
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		String s = "";
		for (Expression e : this.operands)
			s += e.codePrint() + " * ";
		
		s = s.substring(0, s.length() - 3);
		return s;
	}

} 
