package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Source;

public class Compare extends BinaryExpression {

			/* --- NESTED --- */
	public enum COMPARATOR {
		EQUAL, NOT_EQUAL,
		LESS_SAME, LESS_THAN, 
		GREATER_SAME, GREATER_THAN
	}
	
	
			/* --- FIELDS --- */
	public COMPARATOR comparator;
	
	
			/* --- CONSTRUCTORS --- */
	public Compare(Expression left, Expression right, COMPARATOR comparator, Source source) {
		super(left, right, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Compare " + this.comparator.toString());
		
		if (rec) {
			this.getLeft().print(d + this.printDepthStep, rec);
			this.getRight().print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkCompare(this);
	}
	
} 
