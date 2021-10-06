package Imm.AST.Expression.Boolean;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.NFoldExpression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Compare extends NFoldExpression {

			/* ---< NESTED >--- */
	public enum COMPARATOR {
		EQUAL, NOT_EQUAL,
		LESS_SAME, LESS_THAN, 
		GREATER_SAME, GREATER_THAN;
		
		public String toString() {
			String comp = "";
			
			if (this == COMPARATOR.EQUAL) comp = "==";
			if (this == COMPARATOR.GREATER_SAME) comp = ">=";
			if (this == COMPARATOR.GREATER_THAN) comp = ">";
			if (this == COMPARATOR.LESS_SAME) comp = "<=";
			if (this == COMPARATOR.LESS_THAN) comp = "<";
			if (this == COMPARATOR.NOT_EQUAL) comp = "!=";
			
			return comp;
		}

		public COMPARATOR negate() {
			if (this == COMPARATOR.EQUAL) return COMPARATOR.NOT_EQUAL;
			if (this == COMPARATOR.GREATER_SAME) return COMPARATOR.LESS_THAN;
			if (this == COMPARATOR.GREATER_THAN) return COMPARATOR.LESS_SAME;
			if (this == COMPARATOR.LESS_SAME) return COMPARATOR.GREATER_THAN;
			if (this == COMPARATOR.LESS_THAN) return COMPARATOR.GREATER_SAME;
			if (this == COMPARATOR.NOT_EQUAL) return COMPARATOR.EQUAL;
			return null;
		}

		public COMPARATOR flip() {
			if (this == COMPARATOR.EQUAL) return COMPARATOR.EQUAL;
			if (this == COMPARATOR.GREATER_SAME) return COMPARATOR.LESS_SAME;
			if (this == COMPARATOR.GREATER_THAN) return COMPARATOR.LESS_THAN;
			if (this == COMPARATOR.LESS_SAME) return COMPARATOR.GREATER_SAME;
			if (this == COMPARATOR.LESS_THAN) return COMPARATOR.GREATER_THAN;
			if (this == COMPARATOR.NOT_EQUAL) return COMPARATOR.NOT_EQUAL;
			return null;
		}

		public boolean weakerOrSame(COMPARATOR c) {
			if (this == COMPARATOR.EQUAL) return c == this || c == COMPARATOR.GREATER_SAME || c == LESS_SAME;
			if (this == COMPARATOR.NOT_EQUAL) return c == this;
			if (this == COMPARATOR.GREATER_THAN) return c == this || c == GREATER_SAME;
			if (this == COMPARATOR.LESS_THAN) return c == this || c == LESS_SAME;
			return c == this;
		}
	}
	
	
			/* ---< FIELDS >--- */
	public COMPARATOR comparator;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Compare(Expression left, Expression right, COMPARATOR comparator, Source source) {
		super(left, right, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	public Compare(List<Expression> operands, COMPARATOR comparator, Source source) {
		super(operands, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Compare " + this.comparator.toString());
		
		if (rec) {
			for (Expression e : this.operands)
				e.print(d + this.printDepthStep, true);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkCompare(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optCompare(this);
	}
	
	public Compare clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Compare e = new Compare(op0, this.comparator, this.getSource().clone());
		
		if (this.getType() != null)
			e.setType(this.getType().clone());
		
		e.copyDirectivesFrom(this);
		return e;
	}
	
	public String codePrint() {
		String comp = this.comparator.toString();
		return this.operands.stream().map(Expression::codePrint).collect(Collectors.joining(" " + comp + " "));
	}
	
} 
