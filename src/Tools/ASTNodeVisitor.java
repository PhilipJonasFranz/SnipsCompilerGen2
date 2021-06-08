package Tools;

import Imm.AST.SyntaxElement;

public interface ASTNodeVisitor<T extends SyntaxElement> {

	/**
	 * Used as a filter expression. Return true if the given
	 * syntax element is relevant for this search. Then this
	 * element will be added to the returned result set.
	 * @param s The currently inspected syntax element.
	 * @return True if the syntax element is relevant for this filter.
	 */
	boolean visit(SyntaxElement s);
	
}
