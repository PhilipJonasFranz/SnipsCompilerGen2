package Imm.AST.Lhs;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class SimpleLhsId extends LhsId {

			/* ---< FIELDS >--- */
	public IDRef ref;
	
	
			/* ---< CONSTRUCTORS >--- */
	public SimpleLhsId(IDRef ref, Source source) {
		super(source);
		this.ref = ref;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(Util.pad(d) + "SimpleLhsId");
		if (rec) this.ref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIDRef(this.ref);
		this.origin = this.ref.origin;
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public LhsId opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSimpleLhsId(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.ref.visit(visitor));
		
		return result;
	}
	
	public NamespacePath getFieldName() {
		return ref.path;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.ref.setContext(context);
	}

	public SimpleLhsId clone() {
		return new SimpleLhsId((IDRef) this.ref.clone(), this.getSource().clone());
	}

	public String codePrint() {
		return this.ref.codePrint();
	}
	
} 
