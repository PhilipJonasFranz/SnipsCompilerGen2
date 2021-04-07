package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
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
		return ctx.check();
	}

	public Program opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optProgram(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (SyntaxElement s : this.programElements) {
			if (visitor.visit(s))
				result.add((T) s);
		}
		
		return result;
	}
	
	public void setContext(List<TYPE> setContext) {
		return;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		for (SyntaxElement s0 : this.programElements) {
			code.addAll(s0.codePrint(0));
			code.add("");
		}
		
		return code;
	}
	
} 
