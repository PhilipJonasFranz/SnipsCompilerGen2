package Imm.AST.Lhs;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.StructSelect;
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
		CompilerDriver.outs.println(Util.pad(d) + "StructSelectLhsId");
		if (rec) this.select.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkStructSelect(this.select);
		if (this.select.selector instanceof IDRef) {
			IDRef ref = (IDRef) this.select.selector;
			this.origin = ref.origin;
		}
		else if (this.select.selector instanceof ArraySelect) {
			ArraySelect sel = (ArraySelect) this.select.selector;
			this.origin = sel.idRef.origin;
		}
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public LhsId opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optStructSelectLhsId(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.select.visit(visitor));
		
		return result;
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

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.select.setContext(context);
	}

	public StructSelectLhsId clone() {
		StructSelectLhsId lhs = new StructSelectLhsId((StructSelect) this.select.clone(), this.getSource().clone());
		lhs.origin = this.origin;
		
		if (this.expressionType != null)
			lhs.expressionType = this.expressionType.clone();
		
		lhs.copyDirectivesFrom(this);
		return lhs;
	}

	public String codePrint() {
		return this.select.codePrint();
	}

} 
