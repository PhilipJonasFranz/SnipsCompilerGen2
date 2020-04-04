package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class ElementSelect extends Expression {

	private Expression idRef0;
	
	public IDRef idRef;
	
	public List<Expression> selection;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ElementSelect(Expression ref, List<Expression> selection, Source source) {
		super(source);
		this.idRef0 = ref;
		this.selection = selection;
	}
	
	public Expression getShadowRef() {
		return this.idRef0;
	}

	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ElementSelect");
		this.idRef0.print(d + this.printDepthStep, rec);
		for (Expression e : this.selection) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkElementSelect(this);
	}
	
}
