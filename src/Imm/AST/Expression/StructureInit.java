package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.Source;

public class StructureInit extends Expression {

			/* --- FIELDS --- */
	public STRUCT structType;
	
	public List<Expression> elements;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(STRUCT structType, List<Expression> elements, Source source) {
		super(source);
		this.structType = structType;
		this.elements = elements;
	}
	

			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit <" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
		for (Expression e : this.elements) {
			e.print(d + this.printDepthStep, rec);
		}
	}
	
	public void createStructInstance() throws CTX_EXC {
		this.structType = this.structType.typedef.constructStructType(this.structType.proviso);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkStructureInit(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		ProvisoManager.setContext(context, this.structType, this.getSource());
		for (Expression e : this.elements) {
			e.setContext(context);
		}
	}

}
