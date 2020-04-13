package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRefWriteback;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class AssignWriteback extends Statement {

			/* --- FIELDS --- */
	@Getter
	private Expression shadowRef;
	
	public IDRefWriteback idWb;
	
	
			/* --- CONSTRUCTORS --- */
	public AssignWriteback(Expression value, Source source) {
		super(source);
		this.shadowRef = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign Writeback");
		if (rec) {
			this.getShadowRef().print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkAssignWriteback(this);
	}
	
}
