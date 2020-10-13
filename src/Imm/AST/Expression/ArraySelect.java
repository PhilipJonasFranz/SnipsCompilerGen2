package Imm.AST.Expression;

import java.util.List;

import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ArraySelect extends Expression {

			/* --- FIELDS --- */
	/** Expression passed by parser, is context checked to be idref, field idRef will be set to casted ref. */
	private Expression shadowRef;
	
	public IDRef idRef;
	
	public List<Expression> selection;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ArraySelect(Expression ref, List<Expression> selection, Source source) {
		super(source);
		this.shadowRef = ref;
		this.selection = selection;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ArraySelect");
		this.shadowRef.print(d + this.printDepthStep, rec);
		for (Expression e : this.selection) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.shadowRef.setContext(context);
		for (Expression e : this.selection) {
			e.setContext(context);
		}
	}

	public Expression getShadowRef() {
		return this.shadowRef;
	}
	
} 
