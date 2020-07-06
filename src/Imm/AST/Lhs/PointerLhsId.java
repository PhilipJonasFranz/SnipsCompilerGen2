package Imm.AST.Lhs;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class PointerLhsId extends LhsId {

			/* --- FIELDS --- */
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
		if (this.deref != null) this.deref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		if (!(this.shadowDeref instanceof Deref)) {
			throw new CTX_EXCEPTION(this.getSource(), "Left hand identifer is not a dereference");
		}
		else this.deref = (Deref) this.shadowDeref;
		
		this.expressionType = deref.check(ctx);
		return this.expressionType;
	}

	public NamespacePath getFieldName() {
		if (deref.expression instanceof IDRef) {
			return ((IDRef) deref.expression).origin.path;
		}
		else if (deref.expression instanceof ArraySelect) {
			return ((ArraySelect) deref.expression).idRef.origin.path;
		}
		else return null;
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		this.shadowDeref.setContext(context);
	}

	public void releaseContext() {
		this.shadowDeref.releaseContext();
	}
	
	public Expression getShadowDeref() {
		return this.shadowDeref;
	}
	
}
