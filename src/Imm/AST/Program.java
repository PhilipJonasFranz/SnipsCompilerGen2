package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Program extends SyntaxElement {

			/* ---< FIELDS >--- */
	/**
	 * The name of the file this program was parsed from.s
	 */
	public String fileName;
	
	/**
	 * The syntax elements contained in this program.
	 */
	public List<SyntaxElement> programElements;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Program(List<SyntaxElement> programElements, Source source) {
		super(source);
		this.programElements = programElements;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		for (SyntaxElement e : this.programElements) {
			e.print(d, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.check();
		
		ctx.popTrace();
		return t;
	}

	public Program opt(ASTOptimizer opt) throws OPT0_EXC {
		return this;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (SyntaxElement s : this.programElements) {
			result.addAll(s.visit(visitor));
		}
		
		return result;
	}
	
	public void setContext(List<TYPE> setContext) {
		return;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		for (SyntaxElement s0 : this.programElements) {
			List<String> code0 = s0.codePrint(0);
			code.addAll(code0);
			if (!code0.isEmpty()) code.add("");
		}
		
		return code;
	}

	public SyntaxElement clone() {
		return this;
	}
	
} 
