package Imm.AST.Directive;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public abstract class Directive extends SyntaxElement {

			/* --- CONSTRUCTORS --- */
	public Directive(Source source) {
		super(source);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "#directive");
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return new VOID();
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		return;
	}

	public void releaseContext() {
		return;
	}
	
}
