package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.PROVISO;
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

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkSizeOfType(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		if (this.sizeType instanceof PROVISO) {
			PROVISO p = (PROVISO) this.sizeType;
			for (TYPE t : context) {
				if (t.isEqual(p)) {
					p.setContext(t);
					break;
				}
			}
		}
	}

	public void releaseContext() {
		if (this.sizeType instanceof PROVISO) {
			PROVISO p = (PROVISO) this.sizeType;
			p.releaseContext();
		}
	}
	
}
