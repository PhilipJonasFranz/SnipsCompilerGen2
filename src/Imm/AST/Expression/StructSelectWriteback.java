package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class StructSelectWriteback extends Expression {

			/* ---< FIELDS >--- */
	public WRITEBACK writeback;
	
	private Expression shadowSelect;
	
	public StructSelect select;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructSelectWriteback(WRITEBACK idWb, Expression select, Source source) {
		super(source);
		this.writeback = idWb;
		this.shadowSelect = select;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Increment");
		if (rec) this.shadowSelect.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkStructSelectWriteback(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optStructSelectWriteback(this);
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.shadowSelect.setContext(context);
		
		if (this.select != null) 
			this.select.setContext(context);
	}

	public Expression getShadowSelect() {
		return this.shadowSelect;
	}

	public Expression clone() {
		return new StructSelectWriteback(this.writeback, this.shadowSelect.clone(), this.getSource().clone());
	}
	
} 
