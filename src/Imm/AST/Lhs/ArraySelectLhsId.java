package Imm.AST.Lhs;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.ArraySelect;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ArraySelectLhsId extends LhsId {

			/* --- FIELDS --- */
	public ArraySelect selection;
	
	
			/* --- CONSTRUCTORS --- */
	public ArraySelectLhsId(ArraySelect selection, Source source) {
		super(source);
		this.selection = selection;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ElementSelectLhsId");
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
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		this.selection.setContext(context);
	}

	public void releaseContext() {
		this.selection.releaseContext();
	}
	
}
