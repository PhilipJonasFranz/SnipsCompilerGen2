package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

public class InlineCall extends Expression {

	public String functionName;
	
	public Function calledFunction;
	
	public TYPE type;
	
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
