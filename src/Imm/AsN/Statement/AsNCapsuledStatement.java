package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.CapsuledStatement;
import Imm.AST.Statement.ConditionalCapsuledStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;

public abstract class AsNCapsuledStatement extends AsNStatement {

	public AsNCapsuledStatement() {
		
	}
	
	public static AsNCapsuledStatement cast(CapsuledStatement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		if (s instanceof ConditionalCapsuledStatement) {
			return AsNConditionalCapsuledStatement.cast((ConditionalCapsuledStatement) s, r, st);
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No cast available for " + s.getClass().getName());	
	}
	
	/**
	 * Free all loaded declarations that were made in the body of the statement, since
	 * the scope is popped.
	 * @param s The CapsuledStatement containing the Statements.
	 * @param r The current RegSet
	 */
	protected void popDeclarationScope(CapsuledStatement s, RegSet r, StackSet st) {
		List<Declaration> declarations = new ArrayList(); 
		for (Statement s0 : s.body) {
			if (s0 instanceof Declaration) {
				declarations.add((Declaration) s0);
			}
		}
		
		for (Declaration d : declarations) {
			if (r.declarationLoaded(d)) {
				int loc = r.declarationRegLocation(d);
				r.getReg(loc).free();
			}
		}
	}
	
}
