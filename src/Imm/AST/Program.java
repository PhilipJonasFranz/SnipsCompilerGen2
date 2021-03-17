package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
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

	public void setContext(List<TYPE> setContext) {
		return;
	}

} 
