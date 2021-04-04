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
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;

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
		BIT_ORR_ASSIGN, BIT_AND_ASSIGN, BIT_XOR_ASSIGN
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
		System.out.println(this.pad(d) + "Assign, arith = " + this.assignArith.toString());
		
		if (rec) {
			this.lhsId.print(d + this.printDepthStep, rec);
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkAssignment(this);
		
		CompilerDriver.lastSource = temp;
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
		return new Assignment(this.assignArith, this.lhsId.clone(), this.value.clone(), this.getSource().clone());
	}

} 
