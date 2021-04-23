package Imm.AST.Expression.Boolean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Opt.AST.ASTOptimizer;
import Util.Source;

public class And extends BoolNFoldExpression {

			/* ---< CONSTRUCTORS >--- */
	public And(Expression left, Expression right, Source source) {
		super(left, right, Operator.AND, source);
	}
	
	public And(List<Expression> operands, Source source) {
		super(operands, Operator.AND, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAnd(this);
	}
	
	public And clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		And e = new And(op0, this.getSource().clone());
		e.setType(this.getType().clone());
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		return this.operands.stream().map(Expression::codePrint).collect(Collectors.joining(" && "));
	}

} 
