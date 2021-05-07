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

/**
 * This class represents a superclass for all Expressions.
 */
public class AddressOf extends Expression {

			/* ---< FIELDS >--- */
	public Expression expression;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public AddressOf(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "AddressOf");
		if (rec) this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkAddressOf(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAddressOf(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.expression.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.expression.setContext(context);
	}

	public Expression clone() {
		AddressOf aof = new AddressOf(this.expression.clone(), this.getSource().clone());
		
		if (this.getType() != null)
			aof.setType(this.getType().clone());
		
		aof.copyDirectivesFrom(this);
		return aof;
	}

	public String codePrint() {
		return "&" + this.expression.codePrint();
	}

} 
