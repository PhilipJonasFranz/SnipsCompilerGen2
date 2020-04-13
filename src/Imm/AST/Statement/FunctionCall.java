package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

public class FunctionCall extends Statement {

			/* --- FIELDS --- */
	public String functionName;
	
	public Function calledFunction;
	
	public List<Expression> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public FunctionCall(Token functionName, List<Expression> parameters, Source source) {
		super(source);
		this.functionName = functionName.spelling;
		this.parameters = parameters;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Function Call: " + this.functionName);
		for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkFunctionCall(this);
	}
	
}
