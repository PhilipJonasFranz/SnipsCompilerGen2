package Imm.AST.Lhs;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.StructSelect;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructSelectLhsId extends LhsId {

			/* ---< FIELDS >--- */
	public StructSelect select;
	
	
			/* ---< CONSTRUCTORS >--- */
	public StructSelectLhsId(StructSelect select, Source source) {
		super(source);
		this.select = select;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructSelectLhsId");
		if (rec) this.select.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		TYPE t = ctx.checkStructSelect(this.select);
		if (this.select.selector instanceof IDRef) {
			IDRef ref = (IDRef) this.select.selector;
			this.origin = ref.origin;
		}
		else if (this.select.selector instanceof ArraySelect) {
			ArraySelect sel = (ArraySelect) this.select.selector;
			this.origin = sel.idRef.origin;
		}
		return t;
	}

	public NamespacePath getFieldName() {
		if (this.select.selector instanceof IDRef) {
			IDRef ref = (IDRef) this.select.selector;
			return ref.path;
		}
		else if (this.select.selector instanceof ArraySelect) {
			ArraySelect sel = (ArraySelect) this.select.selector;
			return sel.idRef.path;
		}
		else return null;
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.select.setContext(context);
	}

} 
