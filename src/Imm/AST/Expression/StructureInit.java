package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class StructureInit extends Expression {

	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(List<Expression> elements, Source source) {
		super(source);
		this.elements = elements;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit");
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructureInit(this);
	}
	
}
