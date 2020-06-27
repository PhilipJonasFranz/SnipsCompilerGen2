package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public abstract class SyntaxElement {

			/* --- FIELDS --- */
	public List<Directive> directives = new ArrayList();
	
	/** The Tab width when pretty printing [NON-CRITICAL] */
	protected int printDepthStep = 4;
	
	public AsNNode castedNode;
	
	/**
	 * [NON-CRITICAL]<br>
	 * The location of this syntax element in the source code, row and column representation. 
	 */
	Source source;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SyntaxElement(Source source) {
		this.source = source;
	}
	
	
			/* --- METHODS --- */
	/**
	 * [PURE] <br>
	 * Pretty print this syntax element.
	 * @param d The current printing depth, method should be called with 0.
	 * @param rec Wether to recursiveley print the subtree.
	 */
	public abstract void print(int d, boolean rec);
	
	/** 
	 * [PURE] <br>
	 * Create a padding of spaces with the width w 
	 */
	public String pad(int w) {
		String pad = "";
		for (int i = 0; i < w; i++) pad += " ";
		return pad;
	}
	
	public abstract void setContext(List<TYPE> context) throws CTX_EXCEPTION;
	
	public abstract void releaseContext();
	
	/**
	 * Visitor relay for context checking
	 */
	public abstract TYPE check(ContextChecker ctx) throws CTX_EXCEPTION;
	
	public Source getSource() {
		return this.source;
	}
	
}
