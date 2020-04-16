package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.TYPE;
import Util.Source;

public class StructureInit extends Expression {

			/* --- FIELDS --- */
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** A list that contains the combinations of types this function was templated with */
	public List<List<TYPE>> provisosCalls = new ArrayList();
	
	public StructTypedef typedef;
	
	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(StructTypedef typedef, List<TYPE> proviso, List<Expression> elements, Source source) {
		super(source);
		this.typedef = typedef;
		this.provisosTypes = proviso;
		this.elements = elements;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit " + this.getType().typeString());
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructureInit(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		
	}

	public void releaseContext() {
		
	}
	
}
