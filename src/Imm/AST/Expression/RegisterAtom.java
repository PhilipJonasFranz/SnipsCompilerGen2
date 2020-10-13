package Imm.AST.Expression;

import java.util.List;

import Exc.CTX_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class RegisterAtom extends Expression {

			/* --- FIELDS --- */
	/* Type information and potential value */
	public String spelling;
	
	public REG reg;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public RegisterAtom(Token id, Source source) {
		super(source);
		this.setType(new INT());
		this.spelling = id.spelling();
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Register Value <" + this.getType().typeString() + " : " + this.spelling + ">");
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		//System.out.println("Applied Context: " + this.getClass().getName());
		return;
	}

} 
