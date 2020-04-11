package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class PointerLhsId extends LhsId {

			/* --- FIELDS --- */
	public Deref deref;
	
	
			/* --- CONSTRUCTORS --- */
	public PointerLhsId(Deref deref, Source source) {
		super(source);
		this.deref = deref;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "PointerLhsId");
		this.deref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
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
