package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.ElementSelect;
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
		this.deref.print(d, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		TYPE t = ctx.checkElementSelect(this.selection);
		this.origin = this.selection.idRef.origin;
		return t;
	}

	public String getFieldName() {
		return selection.idRef.id;
	}
	
}
