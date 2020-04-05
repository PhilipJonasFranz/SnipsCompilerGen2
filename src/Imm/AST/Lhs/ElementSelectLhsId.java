package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.ElementSelect;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ElementSelectLhsId extends LhsId {

			/* --- FIELDS --- */
	public ElementSelect selection;
	
			/* --- CONSTRUCTORS --- */
	public ElementSelectLhsId(ElementSelect selection, Source source) {
		super(source);
		this.selection = selection;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		this.selection.print(d, true);
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
