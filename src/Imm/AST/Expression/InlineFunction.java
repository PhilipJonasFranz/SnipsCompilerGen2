package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.TYPE.TYPE;
import Util.Source;

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
		System.out.println(this.pad(d) + "Inline Function");
		if (rec) this.inlineFunction.print(d + this.printDepthStep, rec);
	}
	
	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkInlineFunction(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.inlineFunction.setContext(context);
	}

	public Expression clone() {
		return new InlineFunction(this.inlineFunction.clone(), this.getSource().clone());
	}

} 
