package Imm.AST.Expression.Arith;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.NFoldExpression;
import Opt.AST.ASTOptimizer;
import Util.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents a superclass for all Expressions.
 */
public class Div extends NFoldExpression {

	/* Placeholder call, constructed when operands are non-float. */
	public InlineCall placeholderCall;

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Div(Expression left, Expression right, Source source) {
		super(left, right, Operator.DIV, source);
	}

	public Div(List<Expression> operands, Source source) {
		super(operands, Operator.DIV, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDiv(this);
	}
	
	public Div clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Div e = new Div(op0, this.getSource().clone());
		
		if (this.getType() != null)
			e.setType(this.getType().clone());
		
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		return this.operands.stream().map(Expression::codePrint).collect(Collectors.joining(" / "));
	}

} 
