package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ArraySelect extends Expression {

			/* ---< FIELDS >--- */
	/** Expression passed by parser, is context checked to be idref, field idRef will be set to casted ref. */
	private Expression shadowRef;
	
	public IDRef idRef;
	
	public List<Expression> selection;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ArraySelect(Expression ref, List<Expression> selection, Source source) {
		super(source);
		this.shadowRef = ref;
		this.selection = selection;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ArraySelect");
		if (rec) {
			this.shadowRef.print(d + this.printDepthStep, rec);
			
			for (Expression e : this.selection) 
				e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkArraySelect(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optArraySelect(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.shadowRef.visit(visitor));
		
		for (Expression e : this.selection) 
			result.addAll(e.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.shadowRef.setContext(context);
		for (Expression e : this.selection) {
			e.setContext(context);
		}
	}

	public Expression getShadowRef() {
		return this.shadowRef;
	}

	public ArraySelect clone() {
		List<Expression> eclone = new ArrayList();
		for (Expression e : this.selection) eclone.add(e.clone());
		
		return new ArraySelect(this.shadowRef.clone(), eclone, this.getSource().clone());
	}
	
} 
