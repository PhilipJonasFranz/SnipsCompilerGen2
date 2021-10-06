package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Opt.AST.ASTOptimizer;
import Par.Token;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		CompilerDriver.outs.println(Util.pad(d) + "Register Value <" + this.getType() + " : " + this.spelling + ">");
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkRegisterAtom(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optRegisterAtom(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		return;
	}

	public Expression clone() {
		RegisterAtom ra = new RegisterAtom(this.spelling, this.reg, this.getSource().clone());
		ra.setType(this.getType().clone());
		ra.copyDirectivesFrom(this);
		return ra;
	}

	public String codePrint() {
		return "#" + this.reg.toString().toLowerCase();
	}

} 
