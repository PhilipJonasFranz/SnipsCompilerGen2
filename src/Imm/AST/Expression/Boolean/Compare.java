package Imm.AST.Expression.Boolean;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;
import Util.Util;

public class Compare extends NFoldExpression {

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
	
	public Compare(List<Expression> operands, COMPARATOR comparator, Source source) {
		super(operands, Operator.CMP, source);
		this.comparator = comparator;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Compare " + this.comparator.toString());
		
		if (rec) {
			for (Expression e : this.operands)
				e.print(d + this.printDepthStep, rec);
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
	
	public Compare clone() {
		List<Expression> op0 = new ArrayList();
		for (Expression e : this.operands)
			op0.add(e.clone());
		
		Compare e = new Compare(op0, this.comparator, this.getSource().clone());
		e.setType(this.getType().clone());
		return e;
	}
	
	public String codePrint() {
		String comp = "";
		if (this.comparator == COMPARATOR.EQUAL) comp = "==";
		if (this.comparator == COMPARATOR.GREATER_SAME) comp = ">=";
		if (this.comparator == COMPARATOR.GREATER_THAN) comp = ">";
		if (this.comparator == COMPARATOR.LESS_SAME) comp = "<=";
		if (this.comparator == COMPARATOR.LESS_THAN) comp = "<";
		if (this.comparator == COMPARATOR.NOT_EQUAL) comp = "!=";
		
		String s = "";
		for (Expression e : this.operands) 
			s += e.codePrint() + " " + comp + " ";
		
		s = s.substring(0, s.length() - (2 + comp.length()));
		return s;
	}
	
} 
