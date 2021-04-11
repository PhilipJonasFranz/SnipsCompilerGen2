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
public class Add extends NFoldExpression {
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Add(Expression left, Expression right, Source source) {
		super(left, right, Operator.ADD, source);
	}
	
	public Add(List<Expression> operands, Source source) {
		super(operands, Operator.ADD, source);
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAdd(this);
	}
	
	public Add clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Add e = new Add(op0, this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		String s = "";
		for (Expression e : this.operands)
			s += e.codePrint() + " + ";
		
		s = s.substring(0, s.length() - 3);
		return s;
	}
	
} 
