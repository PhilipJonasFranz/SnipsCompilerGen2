package Opt.AST.Util;

import java.util.List;

import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Statement;

public class CompoundStatementRules {

	public static boolean removeDanglingAssignments(List<Statement> body, ProgramState state) {
		boolean opt = false;
		for (int i = 0; i < body.size(); i++) {
			Statement s = body.get(i);
			
			if (s instanceof Assignment) {
				Assignment as = (Assignment) s;
				
				if (as.lhsId instanceof SimpleLhsId) {
					SimpleLhsId lhs = (SimpleLhsId) as.lhsId;
					
					if (lhs.origin != null) {
						
						/*
						 * Only valid if variable was defined in this scope. Variable could be referenced
						 * in a higher-up scope.
						 */
						if (state.isDeclarationScope(lhs.origin)) {
							
							/*
							 * Check if variable was referenced in statement after the current statement.
							 */
							boolean delete = !state.getReferenced(lhs.origin);
							for (int a = i + 1; a < body.size(); a++) 
								delete &= !Matcher.hasRefTo(body.get(a), lhs.origin);

							/*
							 * Make sure removing the expression of the assignment has no side-effects,
							 * for example removing a function call or removing a writeback operation.
							 */
							delete &= !Matcher.containsStateDependentSubExpression(as.value, state);

							/*
							 * No var references have been made, assignment is safe to delete.
							 */
							if (delete) {
								body.remove(i);
								i--;
								opt = true;
							}
						}
					}
				}
			}
		}
		
		return opt;
	}
	
}
