package Imm.AST.Expression.Arith;

import java.util.ArrayList;
import java.util.List;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Opt.AST.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class BitAnd extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BitAnd(Expression left, Expression right, Source source) {
		super(left, right, Operator.BAN, source);
	}
	
	public BitAnd(List<Expression> operands, Source source) {
		super(operands, Operator.BAN, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optBitAnd(this);
	}

	public BitAnd clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		BitAnd e = new BitAnd(op0, this.getSource().clone());
		e.setType(this.getType().clone());
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		String s = "";
		for (Expression e : this.operands)
			s += e.codePrint() + " & ";
		
		s = s.substring(0, s.length() - 3);
		return s;
	}
	
} 
