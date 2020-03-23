package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.CapsuledStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Statement;

public abstract class AsNCapsuledStatement extends AsNStatement {

	public AsNCapsuledStatement() {
		
	}
	
	public static AsNCapsuledStatement cast(CapsuledStatement s, RegSet r) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		if (s instanceof IfStatement) {
			return AsNIfStatement.cast((IfStatement) s, r);
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No cast available for " + s.getClass().getName());	
	}
	
	/**
	 * Free all loaded declarations that were made in the body of the statement, since
	 * the scope is popped.
	 * @param s The CapsuledStatement containing the Statements.
	 * @param r The current RegSet
	 */
	protected void popDeclarationScope(CapsuledStatement s, RegSet r) {
		List<Declaration> declarations = new ArrayList(); 
		for (Statement s0 : s.body) {
			if (s0 instanceof Declaration) {
				declarations.add((Declaration) s0);
			}
		}
		
		for (Declaration d : declarations) {
			if (r.declarationLoaded(d)) {
				int loc = r.declarationRegLocation(d);
				r.regs [loc].free();
			}
		}
	}
	
}
