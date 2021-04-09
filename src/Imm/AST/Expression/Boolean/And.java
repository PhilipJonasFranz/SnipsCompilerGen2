package Imm.AST.Expression.Boolean;

import Exc.OPT0_EXC;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.Expression;
import Opt.ASTOptimizer;
import Util.Source;

public class And extends BoolBinaryExpression {

			/* ---< CONSTRUCTORS >--- */
	public And(Expression left, Expression right, Source source) {
		super(left, right, Operator.AND, source);
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAnd(this);
	}
	
	public NFoldExpression clone() {
		And e = new And(this.left.clone(), this.right.clone(), this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		return this.left.codePrint() + " && " + this.right.codePrint();
	}

} 
