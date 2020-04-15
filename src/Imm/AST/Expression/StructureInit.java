package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.TYPE;
import Util.Source;

public class StructureInit extends Expression {

			/* --- FIELDS --- */
	public StructTypedef typedef;
	
	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(StructTypedef typedef, List<Expression> elements, Source source) {
		super(source);
		this.typedef = typedef;
		this.elements = elements;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit " + this.type.typeString());
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructureInit(this);
	}
	
}
