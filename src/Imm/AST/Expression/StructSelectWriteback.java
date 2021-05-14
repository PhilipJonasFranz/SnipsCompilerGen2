package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		CompilerDriver.outs.println(Util.pad(d) + "Increment");
		if (rec) this.shadowSelect.print(d + this.printDepthStep, true);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkStructSelectWriteback(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) {
		return opt.optStructSelectWriteback(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.shadowSelect.visit(visitor));
		
		return result;
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
		StructSelectWriteback sswb = new StructSelectWriteback(this.writeback, this.shadowSelect.clone(), this.getSource().clone());
		sswb.setType(this.getType().clone());
		sswb.copyDirectivesFrom(this);
		return sswb;
	}

	public String codePrint() {
		String s = this.shadowSelect.codePrint();
		if (this.writeback == WRITEBACK.INCR) s += "++";
		else s += "--";
		return s;
	}
	
} 
