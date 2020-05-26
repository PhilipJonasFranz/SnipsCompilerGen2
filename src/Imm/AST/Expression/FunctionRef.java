package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class FunctionRef extends Expression {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	/* Set during context checking */
	public Function origin;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public FunctionRef(List<TYPE> proviso, NamespacePath path, Source source) {
		super(source);
		this.proviso = proviso;
		this.path = path;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Function Ref: " + this.path.build() + "<" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkFunctionRef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		return;
	}

	public void releaseContext() {
		return;
	}
	
}