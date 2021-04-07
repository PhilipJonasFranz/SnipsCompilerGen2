package Imm.AST.Expression.Boolean;

import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

public class Or extends BoolBinaryExpression {

			/* ---< CONSTRUCTORS >--- */
	public Or(Expression left, Expression right, Source source) {
		super(left, right, Operator.ORR, source);
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optOr(this);
	}
	
	public BinaryExpression clone() {
		Or e = new Or(this.left.clone(), this.right.clone(), this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		return this.left.codePrint() + " || " + this.right.codePrint();
	}
	
} 
