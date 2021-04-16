package Imm.AST.Expression.Boolean;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

public class Ternary extends Expression {
	
			/* ---< FIELDS >--- */
	public Expression condition;
	
	public Expression left;
	
	public Expression right;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Ternary(Expression condition, Expression left, Expression right, Source source) {
		super(source);
		this.condition = condition;
		this.left = left;
		this.right = right;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Ternary");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
			this.left.print(d + this.printDepthStep, rec);
			this.right.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkTernary(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optTernary(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.condition.visit(visitor));
		result.addAll(this.left.visit(visitor));
		result.addAll(this.right.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.condition.setContext(context);
		this.left.setContext(context);
		this.right.setContext(context);
	}

	public Expression clone() {
		Ternary t = new Ternary(this.condition.clone(), this.left.clone(), this.right.clone(), this.getSource().clone());
		t.setType(this.getType().clone());
		t.copyDirectivesFrom(this);
		return t;
	}
	
	public String codePrint() {
		return "(" + this.condition.codePrint() + ")? " + this.left.codePrint() + " : " + this.right.codePrint();
	}

} 
