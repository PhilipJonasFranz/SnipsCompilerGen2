package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Ctx.Util.CheckUtil.Callee;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

public class FunctionCall extends Statement implements Callee {
	
			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	public NamespacePath path;
	
	public Function calledFunction;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	public List<Expression> parameters;
	
	/** Anonymous target. Recieves value during ctx if call is calling a predicate that could not be linked. */
	public Declaration anonTarget;
	
	public boolean hasAutoProviso = false;
	
	public boolean isNestedCall = false;

	public boolean nestedDeref = false;
	
	/** 
	 * This field is only set when this function call is a struct nested call. In this case, 
	 * this field will contain the id ref to the base variable. This field is set automatically
	 * during parsing.
	 */
	public Expression baseRef = null;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public FunctionCall(NamespacePath path, List<TYPE> proviso, List<Expression> parameters, Source source) {
		super(source);
		this.path = path;
		this.proviso = proviso;
		this.parameters = parameters;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "Function Call: " + this.path.build());
		if (this.proviso != null && !this.proviso.isEmpty()) {
			String s = "{";
			for (TYPE t : this.proviso) s += t.typeString() + ", ";
			s = s.substring(0, s.length() - 2);
			s += "}";
			System.out.print(s);
		}
		
		System.out.println();
		
		if (rec) for (Expression e : this.parameters) 
			e.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkCall(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (this.anonTarget == null) {
			for (int i = 0; i < this.proviso.size(); i++) 
				ProvisoUtil.mapNTo1(this.proviso.get(i), context);
		}
		
		for (Expression e : this.parameters) 
			e.setContext(context);
	}


			/* ---< IMPLEMENTATIONS >--- */
	public boolean isNestedCall() {
		return this.isNestedCall;
	}

	public boolean hasAutoProviso() {
		return this.hasAutoProviso;
	}

	public TYPE getType() {
		return this.getType();
	}

	public List<Expression> getParams() {
		return this.parameters;
	}

	public NamespacePath getPath() {
		return this.path;
	}

	public SyntaxElement getCallee() {
		return this;
	}

	public Expression getBaseRef() {
		return this.baseRef;
	}

	public List<TYPE> getProviso() {
		return this.proviso;
	}

	public void setAutoProviso(boolean b) {
		this.hasAutoProviso = b;
	}

	public void setProviso(List<TYPE> proviso) {
		this.proviso = proviso;
	}

	public void setType(TYPE t) {
		return;
	}

	public void setCalledFunction(Function f) {
		this.calledFunction = f;
	}

	public void setWatchpoint(SyntaxElement w) {
		this.watchpoint = w;
	}

	public void setAnonTarget(Declaration d) {
		this.anonTarget = d;
	}

	public boolean isNestedDeref() {
		return this.nestedDeref;
	}
	
} 
