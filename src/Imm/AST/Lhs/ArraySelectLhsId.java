package Imm.AST.Lhs;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.ArraySelect;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
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

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkArraySelect(this.selection);
		this.origin = this.selection.idRef.origin;
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public LhsId opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optArraySelectLhsId(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.selection.visit(visitor));
		
		return result;
	}

	public NamespacePath getFieldName() {
		return selection.idRef.path;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.selection.setContext(context);
	}

	public ArraySelectLhsId clone() {
		return new ArraySelectLhsId((ArraySelect) this.selection.clone(), this.getSource().clone());
	}

} 
