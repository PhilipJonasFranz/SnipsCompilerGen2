package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class RegisterAtom extends Expression {

			/* ---< FIELDS >--- */
	/* Type information and potential value */
	public String spelling;
	
	public REG reg;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public RegisterAtom(Token id, Source source) {
		super(source);
		this.setType(new INT());
		this.spelling = id.spelling();
	}
	
	public RegisterAtom(String spelling, REG reg, Source source) {
		super(source);
		this.spelling = spelling;
		this.reg = reg;
	}


	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Register Value <" + this.getType().typeString() + " : " + this.spelling + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkRegisterAtom(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		//System.out.println("Applied Context: " + this.getClass().getName());
		return;
	}

	public Expression clone() {
		return new RegisterAtom(this.spelling, this.reg, this.getSource().clone());
	}

} 
