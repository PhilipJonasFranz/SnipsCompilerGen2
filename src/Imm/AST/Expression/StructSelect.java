package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

public class StructSelect extends Expression {

			/* ---< FIELDS >--- */
	public Expression selector;
	
	public boolean deref;
	
	public Expression selection;
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructSelect(Expression selector, Expression selection, boolean deref, Source source) {
		super(source);
		this.selection = selection;
		this.selector = selector;
		this.deref = deref;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Struct" + ((this.deref)? "Pointer" : "") + "Select");
		if (rec) {
			this.selector.print(d + this.printDepthStep, true);
			this.selection.print(d + this.printDepthStep, true);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkStructSelect(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optStructSelect(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.selector.visit(visitor));
		result.addAll(this.selection.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.selector.setContext(context);
		this.selection.setContext(context);
	}

	public Expression clone() {
		StructSelect ss = new StructSelect(this.selector.clone(), this.selection.clone(), this.deref, this.getSource().clone());
		
		if (this.getType() != null) 
			ss.setType(this.getType().clone());
		
		ss.copyDirectivesFrom(this);
		return ss;
	}

	public String codePrint() {
		String s = "";
		
		StructSelect c = this;
		
		while (true) {
			s = c.selection.codePrint() + s;
			
			if (this.deref)
				s = "->" + s;
			else s = "." + s;
			
			if (c.selector instanceof StructSelect) {
				c = (StructSelect) c.selector;
			}
			else {
				s = c.selector.codePrint() + s;
				break;
			}
		}
		
		return s;
	}

} 
