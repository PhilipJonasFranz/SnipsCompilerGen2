package Imm.AST.Lhs;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.ArraySelect;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ArraySelectLhsId extends LhsId {

			/* ---< FIELDS >--- */
	public ArraySelect selection;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ArraySelectLhsId(ArraySelect selection, Source source) {
		super(source);
		this.selection = selection;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ElementSelectLhsId");
		if (rec) this.selection.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkArraySelect(this.selection);
		this.origin = this.selection.idRef.origin;
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public NamespacePath getFieldName() {
		return selection.idRef.path;
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.selection.setContext(context);
	}

	public ArraySelectLhsId clone() {
		return new ArraySelectLhsId((ArraySelect) this.selection.clone(), this.getSource().clone());
	}

} 
