package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Ctx.ContextChecker;
import Ctx.Util.Callee;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

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
	public Statement clone() {
		List<TYPE> provClone = new ArrayList();
		for (TYPE t : this.proviso) provClone.add(t.clone());
		
		List<Expression> ec = new ArrayList();
		for (Expression e : this.parameters) ec.add(e.clone());
		
		FunctionCall ic = new FunctionCall(this.path.clone(), provClone, ec, this.getSource().clone());
		ic.calledFunction = this.calledFunction;
		ic.anonTarget = this.anonTarget;
		ic.hasAutoProviso = this.hasAutoProviso;
		ic.isNestedCall = this.isNestedCall;
		ic.nestedDeref = this.nestedDeref;
		
		ic.watchpoint = this.watchpoint;
		
		if (this.baseRef != null)
			ic.baseRef = this.baseRef.clone();
		
		ic.setType(this.getType().clone());
		ic.copyDirectivesFrom(this);
		return ic;
	}
	
	public void print(int d, boolean rec) {
		CompilerDriver.outs.print(Util.pad(d) + "Function Call: " + this.path);
		if (this.proviso != null && !this.proviso.isEmpty()) {
			String s = this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", "{", "}"));
			CompilerDriver.outs.print(s);
		}
		
		CompilerDriver.outs.println();
		
		if (rec) for (Expression e : this.parameters) 
			e.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkCall(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optFunctionCall(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Expression e : this.parameters)
			result.addAll(e.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.anonTarget == null) 
			for (int i = 0; i < this.proviso.size(); i++) 
				ProvisoUtil.mapNTo1(this.proviso.get(i), context);
		
		for (Expression e : this.parameters) 
			e.setContext(context);
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = this.path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::codeString).collect(Collectors.joining(", ", "<", ">"));
		
		s += "(";
		
		if (!this.parameters.isEmpty()) 
			s += this.parameters.stream().map(Expression::codePrint).collect(Collectors.joining(", "));
		
		s += ");";
		code.add(Util.pad(d) + s);
		return code;
	}


			/* ---< IMPLEMENTATIONS >--- */
	public boolean isNestedCall() {
		return this.isNestedCall;
	}

	public boolean hasAutoProviso() {
		return this.hasAutoProviso;
	}

	public TYPE getType() {
		if (this.calledFunction != null)
			return this.calledFunction.getReturnTypeDirect();
		else
			return new VOID();
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

	public void setNestedCall(boolean b) {
		this.isNestedCall = b;
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
	
	public void setPath(NamespacePath path) {
		this.path = path;
	}
	
} 
