package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public abstract class SyntaxElement {

			/* --- FIELDS --- */
	/** The Tab width when pretty printing [NON-CRITICAL] */
	protected int printDepthStep = 4;
	
	public AsNNode castedNode;
	
	/**
	 * [NON-CRITICAL]
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
	
	/** 
	 * [PURE] <br>
	 * Returns the {@link #source} object of this syntax element.
	 */
	public Source getSource() {
		return this.source;
	}
	
	/**
	 * [PURE] 
	 * Create a source code representation of this syntax element. May be called recursiveley. 
	 */
	public abstract List<String> buildProgram(int pad);
	
	/**
	 * Visitor relay for context checking
	 */
	public abstract TYPE check(ContextChecker ctx) throws CTX_EXCEPTION;
	
}
