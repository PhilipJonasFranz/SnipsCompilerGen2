package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

public class FunctionCall extends Statement {

			/* --- FIELDS --- */
	public String functionName;
	
	public Function calledFunction;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	public List<Expression> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public FunctionCall(Token functionName, List<TYPE> provisosTypes, List<Expression> parameters, Source source) {
		super(source);
		this.functionName = functionName.spelling;
		this.provisosTypes = provisosTypes;
		this.parameters = parameters;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "Function Call: " + this.functionName);
		for (TYPE t : this.provisosTypes) System.out.print(", " + t.typeString());
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
		
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			
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
