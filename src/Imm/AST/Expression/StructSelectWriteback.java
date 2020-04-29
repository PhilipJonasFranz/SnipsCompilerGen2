package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.IDRefWriteback.ID_WRITEBACK;
import Imm.TYPE.TYPE;
import Util.Source;
import lombok.Getter;

/**
 * This class represents a superclass for all Expressions.
 */
public class StructSelectWriteback extends Expression {

			/* --- FIELDS --- */
	public ID_WRITEBACK idWb;
	
	@Getter
	private Expression shadowSelect;
	
	public StructSelect select;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructSelectWriteback(ID_WRITEBACK idWb, Expression expression, Source source) {
		super(source);
		this.idWb = idWb;
		this.shadowSelect = select;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Increment");
		this.shadowSelect.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructSelectWriteback(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		this.shadowSelect.setContext(context);
	}

	public void releaseContext() {
		this.shadowSelect.releaseContext();
	}
	
}
