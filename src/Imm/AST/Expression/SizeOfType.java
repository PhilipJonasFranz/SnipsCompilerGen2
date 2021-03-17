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
public class SizeOfType extends Expression {

			/* ---< FIELDS >--- */
	public TYPE sizeType;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfType(TYPE type, Source source) {
		super(source);
		this.sizeType = type;
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		System.out.println(this.pad(d + this.printDepthStep) + this.sizeType.typeString()); 
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkSizeOfType(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
	}

	public Expression clone() {
		return new SizeOfType(this.sizeType.clone(), this.getSource().clone());
	}

} 
