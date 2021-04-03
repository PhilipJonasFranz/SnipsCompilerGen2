package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ArrayInit extends Expression {

			/* ---< FIELDS >--- */
	public List<Expression> elements;
	
	public boolean dontCareTypes = false;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ArrayInit(List<Expression> elements, boolean dontCare, Source source) {
		super(source);
		this.elements = elements;
		this.dontCareTypes = dontCare;
	}
	

			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ArrayInit " + ((this.getType() != null)? this.getType().typeString() : "?"));
		
		if (rec) for (Expression e : this.elements) 
			e.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkArrayInit(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optArrayInit(this);
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		for (Expression e : this.elements) {
			e.setContext(context);
		}
	}

	public Expression clone() {
		List<Expression> eclone = new ArrayList();
		for (Expression e : this.elements) eclone.add(e.clone());
		
		return new ArrayInit(eclone, this.dontCareTypes, this.getSource().clone());
	}

} 
