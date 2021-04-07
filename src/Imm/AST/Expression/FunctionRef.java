package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class FunctionRef extends Expression {

			/* ---< FIELDS >--- */
	public NamespacePath path;
	
	/* Set during context checking */
	public Function origin;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	public IDRef base = null;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public FunctionRef(List<TYPE> proviso, NamespacePath path, Source source) {
		super(source);
		this.proviso = proviso;
		this.path = path;
	}
	
	public FunctionRef(List<TYPE> proviso, Function origin, Source source) {
		super(source);
		this.proviso = proviso;
		this.origin = origin;
		this.path = origin.path;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Function Ref: " + this.path.build() + "<" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkFunctionRef(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optFunctionRef(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		return;
	}

	public Expression clone() {
		List<TYPE> provClone = new ArrayList();
		for (TYPE t : this.proviso) provClone.add(t.clone());
		
		FunctionRef f = new FunctionRef(provClone, this.path.clone(), this.getSource().clone());
		if (this.base != null) f.base = (IDRef) this.base.clone();
		
		f.origin = this.origin;
		f.setType(this.getType().clone());
		
		return f;
	}
	
	public String codePrint() {
		return this.path.build();
	}

} 
