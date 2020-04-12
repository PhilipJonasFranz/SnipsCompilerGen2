package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.AST.Lhs.LhsId;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Assignment extends Statement {

			/* --- NESTED --- */
	public enum ASSIGN_ARITH {
		NONE,
		ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN, MOD_ASSIGN,
		LSL_ASSIGN, LSR_ASSIGN,
		ORR_ASSIGN, AND_ASSIGN, XOR_ASSIGN
	}
	
	
			/* --- FIELDS --- */
	public ASSIGN_ARITH assignArith = ASSIGN_ARITH.NONE;
	
	public LhsId lhsId;
	
	public Declaration origin;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	public Assignment(ASSIGN_ARITH operator, LhsId target, Expression value, Source source) {
		super(source);
		this.assignArith = operator;
		this.lhsId = target;
		this.lhsId.assign = this;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign:" + this.assignArith.toString());
		if (rec) {
			this.lhsId.print(d + this.printDepthStep, rec);
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAssignment(this);
	}
	
}
