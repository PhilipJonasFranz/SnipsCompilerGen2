package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

public class InlineCall extends Expression {

			/* --- FIELDS --- */
	public SyntaxElement watchpoint;
	
	public NamespacePath path;
	
	/** Reference to the AST node of the called function */
	public Function calledFunction;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	/** The Expressions used as parameters */
	public List<Expression> parameters;
	
	/** Anonymous target. Recieves value during ctx if call is calling a predicate that could not be linked. */
	public Declaration anonTarget;
	
	
			/* --- CONSTRUCTORS --- */
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

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + ((this.anonTarget == null)? "Inline" : "Anonymous") + " Call: " + this.path.build());
		if (this.calledFunction != null) {
			for (TYPE t : this.proviso) System.out.print(", " + t.typeString());
			System.out.println(" " + ((this.calledFunction != null)? this.calledFunction.toString().split("@") [1] : "?"));
		}
		else {
			System.out.println();
			anonTarget.print(d + this.printDepthStep, rec);
		}
		for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkInlineCall(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		if (this.anonTarget == null) {
			for (int i = 0; i < this.proviso.size(); i++) {
				TYPE pro = this.proviso.get(i);
				
				if (pro instanceof PROVISO) {
					PROVISO pro0 = (PROVISO) pro;
					for (int a = 0; a < context.size(); a++) {
						/* Found proviso in function head, set context */
						if (context.get(a).isEqual(pro0)) {
							if (context.get(a) instanceof PROVISO) {
								PROVISO pro1 = (PROVISO) context.get(a);
								pro0.setContext(pro1.getContext());
							}
							else pro0.setContext(context.get(a));
							break;
						}
					}
				}
			}
		}
		
		for (Expression e : this.parameters) {
			e.setContext(context);
		}
	}

	public void releaseContext() {
		for (int i = 0; i < this.proviso.size(); i++) {
			TYPE pro = this.proviso.get(i);
			
			if (pro instanceof PROVISO) {
				PROVISO pro0 = (PROVISO) pro;
				pro0.releaseContext();
			}
		}
		
		for (Expression e : this.parameters) {
			e.releaseContext();
		}
	}
	
}
