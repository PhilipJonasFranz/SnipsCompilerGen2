package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Tools.ASTNodeVisitor;
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
	 * Visitor relay for AST optimizer
	 */
	public abstract SyntaxElement opt(ASTOptimizer opt) throws OPT0_EXC;
	
	/**
	 * Visitor filter used to traverse to AST subtree with given filter and 
	 * return a matching result set.
	 * @param <T> Type of the elements contained in the result.
	 * @param visitor The filter expression.
	 * @return The elements the filter returned true for.
	 */
	public abstract <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor);
	
	/**
	 * Print out this node as Snips Code. Each String represents a code-line. The 
	 * outputted code *should* be valid code, so it can be used as compiler input again.
	 * 
	 * The outputted code will represent a snapshot of the current AST. This means that
	 * based on when this representation is created, the AST may has been transformed by
	 * for example the Parser or Context Checker. The outputted code will probably NOT match
	 * the inputted code, since some information about formatting and some other are ignored
	 * in the various stages. Also, internal transformations of the AST will most likely not
	 * be logged, so the AST printout will represent this transformed code.
	 * 
	 * This function is mostly for debug purposes, and can be used to get a more minimal of
	 * the current AST.
	 * 
	 * @param Current printing depth, initially 0.
	 */
	public abstract List<String> codePrint(int d);
	
	/**
	 * Returns the source at which this syntax element was parsed. The Source holds the
	 * approximated source file location from which this AST node was parsed.
	 */
	public Source getSource() {
		return this.source;
	}
	
	/**
	 * Returns the number of nodes that are in the subtree of this syntax element plus 
	 * this syntax element.
	 */
	public int size() {
		return this.visit(x -> { return true; }).size();
	}
	
} 
