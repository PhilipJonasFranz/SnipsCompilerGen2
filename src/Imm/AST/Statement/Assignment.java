package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Lhs.LhsId;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Assignment extends Statement {

			/* ---< NESTED >--- */
	public enum ASSIGN_ARITH {
		
		/* Just assign value */
		NONE,
		
		/* Add, sub... value from existing */
		ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN, MOD_ASSIGN,
		LSL_ASSIGN, LSR_ASSIGN,
		
		/* Boolean Operation */
		ORR_ASSIGN, AND_ASSIGN,
		
		/* Bitwise Operation */
		BIT_ORR_ASSIGN, BIT_AND_ASSIGN, BIT_XOR_ASSIGN;
		
		public String toString() {
			String a = "";
			
			if (this == ASSIGN_ARITH.ADD_ASSIGN) a = "+";
			if (this == ASSIGN_ARITH.AND_ASSIGN) a = "&&";
			if (this == ASSIGN_ARITH.BIT_AND_ASSIGN) a = "&";
			if (this == ASSIGN_ARITH.BIT_ORR_ASSIGN) a = "|";
			if (this == ASSIGN_ARITH.BIT_XOR_ASSIGN) a = "^";
			if (this == ASSIGN_ARITH.DIV_ASSIGN) a = "/";
			if (this == ASSIGN_ARITH.LSL_ASSIGN) a = "<<";
			if (this == ASSIGN_ARITH.LSR_ASSIGN) a = ">>";
			if (this == ASSIGN_ARITH.MOD_ASSIGN) a = "%";
			if (this == ASSIGN_ARITH.MUL_ASSIGN) a = "*";
			if (this == ASSIGN_ARITH.ORR_ASSIGN) a = "||";
			if (this == ASSIGN_ARITH.SUB_ASSIGN) a = "-";
			
			return a;
		}
	}
	
	
			/* ---< FIELDS >--- */
	public ASSIGN_ARITH assignArith = ASSIGN_ARITH.NONE;
	
	/** The LHS that defines the assigning method, f.E direct, assign by dereference... */
	public LhsId lhsId;
	
	/** The value to be assigned. */
	public Expression value;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Assignment(ASSIGN_ARITH operator, LhsId target, Expression value, Source source) {
		super(source);
		this.assignArith = operator;
		this.lhsId = target;
		this.lhsId.assign = this;
		this.value = value;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Assign, arith = " + this.assignArith.toString());
		
		if (rec) {
			this.lhsId.print(d + this.printDepthStep, rec);
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkAssignment(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAssignment(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.lhsId.visit(visitor));
		result.addAll(this.value.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.value.setContext(context);
		this.lhsId.setContext(context);
	}

	public Assignment clone() {
		Assignment assign = new Assignment(this.assignArith, this.lhsId.clone(), this.value.clone(), this.getSource().clone());
		assign.copyDirectivesFrom(this);
		return assign;
	}


	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = this.lhsId.codePrint() + " " + this.assignArith.toString() + "= " + this.value.codePrint() + ";";
		
		code.add(Util.pad(d) + s);
		return code;
	}

} 
