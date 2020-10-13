package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class SizeOfType extends Expression {

			/* --- FIELDS --- */
	public TYPE sizeType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfType(TYPE type, Source source) {
		super(source);
		this.sizeType = type;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		System.out.println(this.pad(d + this.printDepthStep) + this.sizeType.typeString()); 
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkSizeOfType(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
	}

} 
