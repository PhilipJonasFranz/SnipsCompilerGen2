package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Namespace extends SyntaxElement {

			/* ---< FIELDS >--- */
	public NamespacePath path;
	
	public List<SyntaxElement> programElements;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Namespace(NamespacePath path, List<SyntaxElement> programElements, Source source) {
		super(source);
		this.path = path;
		this.programElements = programElements;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Namespace: " + this.path.build());
		if (rec) for (SyntaxElement e : this.programElements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		/* This function should not be called since namespaces are flattened */
		return new VOID();
	}

	public void setContext(List<TYPE> setContext) {
		return;
	}

} 
