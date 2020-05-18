package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

public class FunctionCall extends Statement {

			/* --- FIELDS --- */
	public SyntaxElement watchpoint;
	
	public NamespacePath path;
	
	public Function calledFunction;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	public List<Expression> parameters;
	
	
			/* --- CONSTRUCTORS --- */
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

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "Function Call: " + this.path.build());
		if (!this.proviso.isEmpty()) {
			String s = "{";
			for (TYPE t : this.proviso) s += t.typeString() + ", ";
			s = s.substring(0, s.length() - 2);
			s += "}";
			System.out.print(s);
		}
		System.out.println();
		for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkFunctionCall(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		
		for (int i = 0; i < this.proviso.size(); i++) {
			TYPE pro = this.proviso.get(i);
			
			if (pro instanceof PROVISO) {
				PROVISO pro0 = (PROVISO) pro;
				for (int a = 0; a < context.size(); a++) {
					/* Found proviso in function head, set context */
					if (context.get(a).isEqual(pro0)) {
						pro0.setContext(context.get(a));
						break;
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
