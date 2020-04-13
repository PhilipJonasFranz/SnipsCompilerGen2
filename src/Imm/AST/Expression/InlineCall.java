package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

public class InlineCall extends Expression {

			/* --- FIELDS --- */
	/** The name of the called function */
	public String functionName;
	
	/** Reference to the AST node of the called function */
	public Function calledFunction;
	
	/** Set to the return type of the called function */
	public TYPE type;
	
	/** The Expressions used as parameters */
	public List<Expression> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InlineCall(Token functionName, List<Expression> parameters, Source source) {
		super(source);
		this.functionName = functionName.spelling;
		this.parameters = parameters;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Inline Call: " + this.functionName);
		for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkInlineCall(this);
	}
	
}
