package Imm.AST.Lhs;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class PointerLhsId extends LhsId {

			/* ---< FIELDS >--- */
	private Expression shadowDeref;
	
	public Deref deref;
	
	
			/* ---< CONSTRUCTORS >--- */
	public PointerLhsId(Expression deref, Source source) {
		super(source);
		this.shadowDeref = deref;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "PointerLhsId");
		if (this.deref != null && rec) this.deref.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		if (!(this.shadowDeref instanceof Deref)) {
			throw new CTEX_EXC(this, "Left hand identifer is not a dereference");
		}
		else this.deref = (Deref) this.shadowDeref;
		
		this.expressionType = deref.check(ctx);
		
		ctx.popTrace();
		return this.expressionType;
	}
	
	public LhsId opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optPointerLhsId(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.shadowDeref.visit(visitor));
		
		return result;
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
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.shadowDeref.setContext(context);
	}

	public Expression getShadowDeref() {
		return this.shadowDeref;
	}

	public PointerLhsId clone() {
		PointerLhsId lhs = new PointerLhsId(this.shadowDeref.clone(), this.getSource().clone());
		
		if (this.deref != null)
			lhs.deref = (Deref) lhs.shadowDeref;
		
		lhs.origin = this.origin;
		
		if (this.expressionType != null)
			lhs.expressionType = this.expressionType.clone();
		
		lhs.copyDirectivesFrom(this);
		return lhs;
	}

	public String codePrint() {
		return this.shadowDeref.codePrint();
	}
	
} 
