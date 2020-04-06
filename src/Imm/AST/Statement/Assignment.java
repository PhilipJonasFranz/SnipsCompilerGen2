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

			/* --- FIELDS --- */
	public LhsId lhsId;
	
	public Declaration origin;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	public Assignment(LhsId target, Expression value, Source source) {
		super(source);
		this.lhsId = target;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign");
		if (rec) {
			this.lhsId.print(d + this.printDepthStep, rec);
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAssignment(this);
	}
	
}
