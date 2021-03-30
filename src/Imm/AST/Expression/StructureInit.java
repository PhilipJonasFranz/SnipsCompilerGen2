package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Snips.CompilerDriver;
import Util.Source;

public class StructureInit extends Expression {

			/* ---< FIELDS >--- */
	public STRUCT structType;
	
	public List<Expression> elements;
	
	public boolean hasCoveredParam = false;
	
	public boolean isTopLevelExpression = true;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructureInit(STRUCT structType, List<Expression> elements, Source source) {
		super(source);
		this.structType = structType;
		this.elements = elements;
	}
	

			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "StructureInit <" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
		
		if (rec) for (Expression e : this.elements) 
			e.print(d + this.printDepthStep, rec);
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkStructureInit(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.structType, context);
		
		for (Expression e : this.elements) 
			e.setContext(context);
	}

	public Expression clone() {
		List<Expression> ec = new ArrayList();
		for (Expression e : this.elements) ec.add(e.clone());
		
		StructureInit in = new StructureInit(this.structType.clone(), ec, this.getSource().clone());
		in.hasCoveredParam = this.hasCoveredParam;
		in.isTopLevelExpression = this.isTopLevelExpression;
		
		return in;
	}

} 
