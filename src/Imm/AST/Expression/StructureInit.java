package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.Source;

public class StructureInit extends Expression {

			/* --- FIELDS --- */
	/** List of the provisos types this function is templated with */
	public List<TYPE> proviso;
	
	private StructTypedef typedef;
	
	public STRUCT structType;
	
	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(StructTypedef typedef, List<TYPE> proviso, List<Expression> elements, Source source) {
		super(source);
		this.typedef = typedef;
		this.proviso = proviso;
		this.elements = elements;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit <" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}
	
	public void createStructInstance() throws CTX_EXCEPTION {
		this.structType = this.typedef.constructStructType(this.proviso);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructureInit(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		
	}

	public void releaseContext() {
		
	}
	
}