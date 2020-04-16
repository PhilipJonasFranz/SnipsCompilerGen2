package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

public class InlineCall extends Expression {

			/* --- FIELDS --- */
	/** The name of the called function */
	public String functionName;
	
	/** Reference to the AST node of the called function */
	public Function calledFunction;
	
	public Function caller;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** The Expressions used as parameters */
	public List<Expression> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InlineCall(Token functionName, List<TYPE> proviso, List<Expression> parameters, Source source) {
		super(source);
		this.functionName = functionName.spelling;
		this.provisosTypes = proviso;
		this.parameters = parameters;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "Inline Call: " + this.functionName);
		for (TYPE t : this.provisosTypes) System.out.print(", " + t.typeString());
		System.out.println();
		for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkInlineCall(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			
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
		
		for (Expression e : this.parameters) {
			e.setContext(context);
		}
	}

	public void releaseContext() {
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			
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
