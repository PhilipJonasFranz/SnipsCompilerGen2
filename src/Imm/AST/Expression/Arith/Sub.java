package Imm.AST.Expression.Arith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Opt.AST.ASTOptimizer;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Sub extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Sub(Expression left, Expression right, Source source) {
		super(left, right, Operator.SUB, source);
	}
	
	public Sub(List<Expression> operands, Source source) {
		super(operands, Operator.SUB, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSub(this);
	}
	
	public Sub clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Sub e = new Sub(op0, this.getSource().clone());
		
		if (this.getType() != null)
			e.setType(this.getType().clone());
		
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		return this.operands.stream().map(Expression::codePrint).collect(Collectors.joining(" - "));
	}

} 
