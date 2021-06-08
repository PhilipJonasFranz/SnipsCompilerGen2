package Imm.AST.Expression;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		CompilerDriver.outs.println(Util.pad(d) + "StructureInit <" + ((this.getType() != null)? this.getType() : "?") + ">");
		
		if (rec) for (Expression e : this.elements) 
			e.print(d + this.printDepthStep, true);
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkStructureInit(this);
		
		ctx.popTrace();
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

		/*
		 * Apply context to fabricated struct type, since
		 * its proviso are used to load the correct SID when calling
		 * 'structureInit' in AsNStructureInit.
		 */
		ProvisoUtil.mapNTo1(this.type, context);

		for (Expression e : this.elements) 
			e.setContext(context);
	}

	public Expression clone() {
		List<Expression> ec = new ArrayList();
		for (Expression e : this.elements) ec.add(e.clone());
		
		StructureInit in = new StructureInit(this.structType.clone(), ec, this.getSource().clone());
		in.hasCoveredParam = this.hasCoveredParam;
		in.isTopLevelExpression = this.isTopLevelExpression;
		
		in.setType(this.getType().clone());
		
		in.copyDirectivesFrom(this);
		return in;
	}

	public String codePrint() {
		String s = this.structType.getTypedef().path + "::(";
		s += this.elements.stream().map(Expression::codePrint).collect(Collectors.joining(", "));
		s += ")";
		return s;
	}

} 
