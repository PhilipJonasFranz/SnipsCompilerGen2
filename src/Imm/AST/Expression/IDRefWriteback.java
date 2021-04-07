package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRefWriteback extends Expression {
	
			/* ---< FIELDS >--- */
	public WRITEBACK writeback;
	
	private Expression shadowRef;
	
	public IDRef idRef;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRefWriteback(WRITEBACK idWb, Expression expression, Source source) {
		super(source);
		this.writeback = idWb;
		this.shadowRef = expression;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Increment");
		if (rec) this.shadowRef.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIDRefWriteback(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optIDRefWriteback(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.shadowRef.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.shadowRef.setContext(context);
	}

	public Expression getShadowRef() {
		return this.shadowRef;
	}

	public Expression clone() {
		IDRefWriteback idwb = new IDRefWriteback(this.writeback, this.shadowRef.clone(), this.getSource().clone());
		idwb.setType(this.getType().clone());
		return idwb;
	}

	public String codePrint() {
		String s = this.shadowRef.codePrint();
		if (this.writeback == WRITEBACK.INCR) s += "++";
		else s += "--";
		return s;
	}
	
} 
