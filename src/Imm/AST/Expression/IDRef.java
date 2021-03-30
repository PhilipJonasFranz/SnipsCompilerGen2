package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Source;

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
		System.out.println(this.pad(d) + "IDRef: " + this.path.build() + "<" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIDRef(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.origin != null) 
			this.setType(this.origin.getType().clone());
		
		ProvisoUtil.mapNTo1(this.getType(), context);
	}

	public Expression clone() {
		IDRef r = new IDRef(this.path.clone(), this.getSource().clone());
		r.origin = this.origin;
		return r;
	}

} 
