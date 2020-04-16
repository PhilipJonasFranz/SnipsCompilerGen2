package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Program extends SyntaxElement {

			/* --- FIELDS --- */
	public String fileName;
	
	public List<SyntaxElement> programElements;
	
	public List<Function> functions = new ArrayList();
	
	public List<STRUCT> structs = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Program(List<SyntaxElement> programElements, Source source) {
		super(source);
		this.programElements = programElements;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		for (Directive dir : this.directives) dir.print(d, rec);
		for (SyntaxElement e : this.programElements) {
			e.print(d, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.check();
	}

	public void setContext(List<TYPE> setContext) {
		return;
	}

	public void releaseContext() {
		return;
	}
	
}
