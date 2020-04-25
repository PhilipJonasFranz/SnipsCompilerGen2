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
public class SizeOfExpression extends Expression {

			/* --- FIELDS --- */
	public Expression expression;
	
	public TYPE sizeType;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SizeOfExpression(Expression expression, Source source) {
		super(source);
		this.expression = expression;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "SizeOf");
		this.expression.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkSizeOfExpression(this);
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
		
		this.expression.setContext(context);
	}

	public void releaseContext() {
		if (this.sizeType instanceof PROVISO) {
			PROVISO p = (PROVISO) this.sizeType;
			p.releaseContext();
		}
		
		this.expression.releaseContext();
	}
	
}
