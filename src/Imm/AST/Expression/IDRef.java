package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRef extends Expression {

			/* ---< FIELDS >--- */
	public NamespacePath path;
	
	/* Set during context checking */
	public Declaration origin;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRef(NamespacePath path, Source source) {
		super(source);
		this.path = path;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "IDRef: " + this.path + "<" + ((this.getType() != null)? this.getType() : "?") + ">");
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkIDRef(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optIDRef(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}
	
	public void setContext(List<TYPE> context) {
		if (this.origin != null) 
			this.setType(this.origin.getType().clone());
		
		ProvisoUtil.mapNTo1(this.getType(), context);
	}

	public Expression clone() {
		IDRef r = new IDRef(this.path.clone(), this.getSource().clone());
		r.origin = this.origin;
		
		if (this.getType() != null) 
			r.setType(this.getType().clone());
		
		r.copyDirectivesFrom(this);
		return r;
	}

	public String codePrint() {
		return this.path.build();
	}

} 
