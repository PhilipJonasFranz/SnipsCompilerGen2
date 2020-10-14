package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class StructSelectWriteback extends Expression {

			/* --- FIELDS --- */
	public WRITEBACK writeback;
	
	private Expression shadowSelect;
	
	public StructSelect select;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructSelectWriteback(WRITEBACK idWb, Expression select, Source source) {
		super(source);
		this.writeback = idWb;
		this.shadowSelect = select;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Increment");
		if (rec) this.shadowSelect.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkStructSelectWriteback(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.shadowSelect.setContext(context);
		this.select.setContext(context);
	}

	public Expression getShadowSelect() {
		return this.shadowSelect;
	}
	
	
} 
