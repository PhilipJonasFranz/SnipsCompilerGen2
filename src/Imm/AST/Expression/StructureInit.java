package Imm.AST.Expression;

import java.util.List;

import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.Source;

public class StructureInit extends Expression {

			/* --- FIELDS --- */
	public STRUCT structType;
	
	public List<Expression> elements;
	
	public boolean hasCoveredParam = false;
	
	public boolean isTopLevelExpression = true;
	
	
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
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		ProvisoUtil.mapNTo1(this.structType, context);
		
		for (Expression e : this.elements) 
			e.setContext(context);
	}

} 
