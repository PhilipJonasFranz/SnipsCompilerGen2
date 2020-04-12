package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class PointerLhsId extends LhsId {

			/* --- FIELDS --- */
	@Getter
	private Expression shadowDeref;
	
	public Deref deref;
	
	
			/* --- CONSTRUCTORS --- */
	public PointerLhsId(Expression deref, Source source) {
		super(source);
		this.shadowDeref = deref;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "PointerLhsId");
		this.deref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		if (!(this.shadowDeref instanceof Deref)) {
			throw new CTX_EXCEPTION(this.getSource(), "Left hand identifer is not a dereference");
		}
		else this.deref = (Deref) this.shadowDeref;
		
		TYPE t = ctx.checkExpression(deref.expression);
		return t;
	}

	public String getFieldName() {
		if (deref.expression instanceof IDRef) {
			return ((IDRef) deref.expression).origin.fieldName;
		}
		else if (deref.expression instanceof ElementSelect) {
			return ((ElementSelect) deref.expression).idRef.origin.fieldName;
		}
		else return null;
	}
	
}
