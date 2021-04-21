package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all Expressions.
 */
public class Atom extends Expression {

			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Atom(TYPE type, Source source) {
		super(source);
		this.setType(type);
	}

	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		Integer value = this.getType().toInt();
		CompilerDriver.outs.println(Util.pad(d) + "Atom <" + this.getType().typeString() + ">" + ((value != null)? " " + value : ""));
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkAtom(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAtom(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this)) result.add((T) this);
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		
	}

	public Expression clone() {
		Atom atom = new Atom(this.getType().clone(), this.getSource().clone());
		atom.setType(this.getType().clone());
		atom.copyDirectivesFrom(this);
		return atom;
	}

	public String codePrint() {
		if (this.getType().value != null)
			return this.getType().value.toString();
		else return this.getType().codeString();
	}

} 
