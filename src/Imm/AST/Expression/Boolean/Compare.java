package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

public class Compare extends BinaryExpression {

			/* ---< NESTED >--- */
	public enum COMPARATOR {
		EQUAL, NOT_EQUAL,
		LESS_SAME, LESS_THAN, 
		GREATER_SAME, GREATER_THAN
	}
	
	
			/* ---< FIELDS >--- */
	public COMPARATOR comparator;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Compare(Expression left, Expression right, COMPARATOR comparator, Source source) {
		super(left, right, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Compare " + this.comparator.toString());
		
		if (rec) {
			this.getLeft().print(d + this.printDepthStep, rec);
			this.getRight().print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkCompare(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optCompare(this);
	}
	
	public BinaryExpression clone() {
		return new Compare(this.left.clone(), this.right.clone(), this.comparator, this.getSource().clone());
	}
	
} 
