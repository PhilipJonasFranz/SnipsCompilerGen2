package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public abstract class SyntaxElement {

			/* ---< FIELDS >--- */
	/** 
	 * The Tab width when pretty printing. 
	 */
	protected int printDepthStep = 4;
	
	/**
	 * The AsNNode that was casted from this syntax element.
	 * Field is set once casting is begun/finished.
	 */
	public AsNNode castedNode;
	
	/**
	 * The location of this syntax element in the source code, row and column representation. 
	 */
	Source source;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public SyntaxElement(Source source) {
		this.source = source;
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * Pretty print this syntax element.
	 * @param d The current printing depth, method should be called with 0.
	 * @param rec Wether to recursiveley print the subtree.
	 */
	public abstract void print(int d, boolean rec);

	/**
	 * Applies given context to contained proviso types if available.
	 * Also applies context recursiveley to the entire subtree.
	 * @param context The provided Context.
	 */
	public abstract void setContext(List<TYPE> context) throws CTEX_EXC;
	
	/**
	 * Visitor relay for context checking
	 */
	public abstract TYPE check(ContextChecker ctx) throws CTEX_EXC;
	
	/** 
	 * Create a padding of spaces with the given length.
	 * For example w=3 -> '   '.
	 */
	public String pad(int w) {
		String pad = "";
		for (int i = 0; i < w; i++) pad += " ";
		return pad;
	}
	
	/**
	 * Returns the source at which this syntax element was parsed.
	 */
	public Source getSource() {
		return this.source;
	}
	
} 
