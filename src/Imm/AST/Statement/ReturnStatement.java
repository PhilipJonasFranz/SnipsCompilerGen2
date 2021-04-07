package Imm.AST.Statement;

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

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ReturnStatement extends Statement {

			/* ---< FIELDS >--- */
	public Expression value;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public ReturnStatement(Expression value, Source source) {
		super(source);
		this.value = value;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Return");
		
		if (rec && this.value != null) 
			this.value.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkReturn(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optReturnStatement(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		if (this.value != null)
			result.addAll(this.value.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (this.value != null) 
			this.value.setContext(context);
	}

	public Statement clone() {
		return new ReturnStatement((this.value != null)? this.value.clone() : null, this.getSource().clone());
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "return";
		if (this.value != null) 
			s += " " + this.value.codePrint();
		s += ";";
		code.add(Util.pad(d) + s);
		return code;
	}

} 
