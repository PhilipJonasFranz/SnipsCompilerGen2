package Imm.AST.Expression.Boolean;

import java.util.ArrayList;
import java.util.List;

import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

public class Or extends BoolNFoldExpression {

			/* ---< CONSTRUCTORS >--- */
	public Or(Expression left, Expression right, Source source) {
		super(left, right, Operator.ORR, source);
	}

	public Or(List<Expression> operands, Source source) {
		super(operands, Operator.ORR, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optOr(this);
	}
	
	public Or clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Or e = new Or(op0, this.getSource().clone());
		e.setType(this.getType().clone());
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		String s = "";
		for (Expression e : this.operands)
			s += e.codePrint() + " || ";
		
		s = s.substring(0, s.length() - 4);
		return s;
	}
	
} 
