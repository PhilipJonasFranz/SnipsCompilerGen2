package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.ArraySelect;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructSelectLhsId extends LhsId {

			/* --- FIELDS --- */
	public ArraySelect selection;
	
	
			/* --- CONSTRUCTORS --- */
	public StructSelectLhsId(ArraySelect selection, Source source) {
		super(source);
		this.selection = selection;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructSelectLhsId");
		this.selection.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		TYPE t = ctx.checkArraySelect(this.selection);
		this.origin = this.selection.idRef.origin;
		return t;
	}

	public String getFieldName() {
		return selection.idRef.id;
	}
	
	ERROR
	
}
