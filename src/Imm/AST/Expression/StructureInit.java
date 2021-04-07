package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		System.out.println(Util.pad(d) + "StructureInit <" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
		
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
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optStructureInit(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Expression e : this.elements)
			result.addAll(e.visit(visitor));
		
		return result;
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

	public String codePrint() {
		String s = this.structType.getTypedef().path.build() + "::(";
		for (Expression e : this.elements)
			s += e.codePrint() + ", ";
		s = s.substring(0, s.length() - 2);
		s += ")";
		return s;
	}

} 
