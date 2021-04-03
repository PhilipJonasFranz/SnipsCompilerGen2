package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

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
		System.out.println(this.pad(d) + "Placeholder Atom <" + this.getType().typeString() + ">");
		System.out.println(this.pad(d) + "Inherited Type <" + ((this.inheritType != null)? this.inheritType.typeString() : "?") + ">");
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

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.inheritType != null)
			ProvisoUtil.mapNTo1(this.inheritType, context);
		
		this.base.setContext(context);
	}

	public Expression clone() {
		TempAtom t = new TempAtom(this.base.clone(), this.getSource().clone());
		if (this.inheritType != null) t.inheritType = this.inheritType.clone();
		return t;
	}

} 
