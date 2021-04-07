package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		System.out.println(Util.pad(d) + "SizeOf");
		System.out.println(Util.pad(d + this.printDepthStep) + this.sizeType.typeString()); 
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkSizeOfType(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Expression opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optSizeOfType(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		ProvisoUtil.mapNTo1(this.sizeType, context);
	}

	public Expression clone() {
		return new SizeOfType(this.sizeType.clone(), this.getSource().clone());
	}

	public String codePrint() {
		return "sizeOf(" + this.sizeType.codeString() + ")";
	}

} 
