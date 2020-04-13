package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public class ElementSelect extends Expression {

			/* --- FIELDS --- */
	/** Expression passed by parser, is context checked to be idref, field idRef will be set to casted ref. */
	@Getter
	private Expression shadowRef;
	
	public IDRef idRef;
	
	public List<Expression> selection;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ElementSelect(Expression ref, List<Expression> selection, Source source) {
		super(source);
		this.shadowRef = ref;
		this.selection = selection;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ElementSelect");
		this.shadowRef.print(d + this.printDepthStep, rec);
		for (Expression e : this.selection) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkElementSelect(this);
	}
	
}
