package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDOfExpression extends Expression {

			/* ---< FIELDS >--- */
	public TYPE type;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDOfExpression(TYPE type, Source source) {
		super(source);
		this.type = type;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "IDOf " + this.type.typeString());
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkIDOfExpression(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.type, context);
	}

	public Expression clone() {
		return new IDOfExpression(this.type.clone(), this.getSource().clone());
	}

} 
