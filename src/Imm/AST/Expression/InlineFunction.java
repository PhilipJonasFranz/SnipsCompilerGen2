package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class InlineFunction extends Expression {

			/* ---< FIELDS >--- */
	public Function inlineFunction;
	
	
			/* ---< CONSTRUCTORS >--- */
	public InlineFunction(Function inlineFunction, Source source) {
		super(source);
		this.inlineFunction = inlineFunction;
	}
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Inline Function");
		if (rec) this.inlineFunction.print(d + this.printDepthStep, rec);
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkInlineFunction(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optInlineFunction(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.inlineFunction.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.inlineFunction.setContext(context);
	}

	public Expression clone() {
		InlineFunction if0 = new InlineFunction(this.inlineFunction.clone(), this.getSource().clone());
		
		if (this.getType() != null)
			if0.setType(this.getType().clone());
		
		if0.copyDirectivesFrom(this);
		return if0;
	}

	public String codePrint() {
		throw new SNIPS_EXC("Not implemented!");
	}

} 
