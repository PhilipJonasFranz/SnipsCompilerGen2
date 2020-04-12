package Imm.AST.Lhs;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SimpleLhsId extends LhsId {

			/* --- FIELDS --- */
	public IDRef ref;
	
			/* --- CONSTRUCTORS --- */
	public SimpleLhsId(IDRef ref, Source source) {
		super(source);
		this.ref = ref;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SimpleLhsId");
		this.ref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		TYPE t = ctx.checkIDRef(this.ref);
		this.origin = this.ref.origin;
		return t;
	}

	public String getFieldName() {
		return ref.id;
	}
	
}
