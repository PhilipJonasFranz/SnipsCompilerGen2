package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Namespace extends SyntaxElement {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	public List<SyntaxElement> programElements;
	
	public List<Namespace> namespaces = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Namespace(NamespacePath path, List<SyntaxElement> programElements, Source source) {
		super(source);
		this.path = path;
		this.programElements = programElements;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Namespace: " + this.path.build());
		for (Directive dir : this.directives) dir.print(d + this.printDepthStep, rec);
		for (SyntaxElement e : this.programElements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkNamespace(this);
	}

	public void setContext(List<TYPE> setContext) {
		return;
	}

	public void releaseContext() {
		return;
	}
	
}
