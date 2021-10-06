package Imm.AST.Expression;

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
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.ASTDirective;
import Util.ASTDirective.DIRECTIVE;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

public class InlineCall extends Expression implements Callee {

			/* ---< FIELDS >--- */
	public SyntaxElement watchpoint;
	
	public NamespacePath path;
	
	/** Reference to the AST node of the called function */
	public Function calledFunction;
	
	/** List of the provisos types this call is templated with */
	public List<TYPE> proviso;
	
	/** The Expressions used as parameters */
	public List<Expression> parameters;
	
	/** Anonymous target. Recieves value during ctx if call is calling a predicate that could not be linked. */
	public Declaration anonTarget;
	
	public boolean hasAutoProviso = false;
	
	public boolean isNestedCall = false;
	
	public boolean nestedDeref = false;
	
	public int INLINE_DEPTH = -1;

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InlineCall(NamespacePath path, List<TYPE> proviso, List<Expression> parameters, Source source) {
		super(source);
		this.path = path;
		this.proviso = proviso;
		this.parameters = parameters;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.print(Util.pad(d) + ((this.anonTarget == null)? "" : "Anonymous ") + "Inline Call: " + this.path);
		if (this.calledFunction != null) {
			for (TYPE t : this.proviso) CompilerDriver.outs.print(", " + t);
			CompilerDriver.outs.println(" " + ((this.calledFunction != null)? this.calledFunction.toString().split("@") [1] : "?"));
		}
		else {
			CompilerDriver.outs.println();
			if (anonTarget != null) anonTarget.print(d + this.printDepthStep, rec);
			else CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + "Target:?");
		}
		
		if (rec) for (Expression e : this.parameters) 
			e.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkCall(this);
		
		if (this.calledFunction != null) {
			Function called = this.calledFunction;
			
			if (called.hasDirective(DIRECTIVE.INLINE)) {
				ASTDirective directive = called.getDirective(DIRECTIVE.INLINE);
				if (directive.hasProperty("depth")) {
					int depth = Integer.parseInt(directive.getProperty("depth"));
					this.INLINE_DEPTH = depth;
				}
			}
		}
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optInlineCall(this);
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
		/* If func head exists */
		if (this.anonTarget == null) 
			for (int i = 0; i < this.proviso.size(); i++) 
				ProvisoUtil.mapNTo1(this.proviso.get(i), context);
		
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
		return null;
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

	public Expression clone() {
		List<TYPE> provClone = new ArrayList();
		for (TYPE t : this.proviso) provClone.add(t.clone());
		
		List<Expression> ec = new ArrayList();
		for (Expression e : this.parameters) ec.add(e.clone());
		
		InlineCall ic = new InlineCall(this.path.clone(), provClone, ec, this.getSource().clone());
		ic.calledFunction = this.calledFunction;
		ic.anonTarget = this.anonTarget;
		ic.hasAutoProviso = this.hasAutoProviso;
		ic.isNestedCall = this.isNestedCall;
		ic.nestedDeref = this.nestedDeref;
		
		ic.watchpoint = this.watchpoint;
		
		ic.setType(this.getType().clone());
		
		ic.copyDirectivesFrom(this);
		ic.INLINE_DEPTH = this.INLINE_DEPTH;
		return ic;
	}

	public String codePrint() {
		String s = this.path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::codeString).collect(Collectors.joining(", ", "<", ">"));
		
		s += "(";
		
		if (!this.parameters.isEmpty()) 
			s += this.parameters.stream().map(Expression::codePrint).collect(Collectors.joining(", "));
		
		s += ")";
		return s;
	}

	public void setPath(NamespacePath path) {
		this.path = path;
	}
	
} 
