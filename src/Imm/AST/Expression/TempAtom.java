package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class TempAtom extends Expression {

			/* ---< FIELDS >--- */
	/**
	 * The expression which evaluates the value the stack is filled with.
	 */
	public Expression base;
	
	/**
	 * The type this placeholder atom replaces.
	 */
	public TYPE inheritType;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public TempAtom(Expression base, Source source) {
		super(source);
		this.base = base;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Placeholder Atom <" + this.getType().typeString() + ">");
		CompilerDriver.outs.println(Util.pad(d) + "Inherited Type <" + ((this.inheritType != null)? this.inheritType.typeString() : "?") + ">");
		if (rec && this.base != null) this.base.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkTempAtom(this);
		
		if (this.inheritType == null)
			throw new CTEX_EXC(this.getSource(), "Placeholder atom is not available at this location");
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optTempAtom(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		if (this.base != null)
			result.addAll(this.base.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.inheritType != null)
			ProvisoUtil.mapNTo1(this.inheritType, context);
		
		this.base.setContext(context);
	}

	public Expression clone() {
		TempAtom t = new TempAtom(((this.base != null)? this.base.clone() : null), this.getSource().clone());
		if (this.inheritType != null) 
			t.inheritType = this.inheritType.clone();
		
		t.setType(this.getType().clone());
		t.copyDirectivesFrom(this);
		return t;
	}

	public String codePrint() {
		if (this.base == null) return "...";
		else return "(" + this.base.codePrint() + ")...";
	}

} 
