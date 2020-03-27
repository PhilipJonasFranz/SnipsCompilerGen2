package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Return;
import Imm.AST.Statement.Statement;
import Imm.AsN.AsNNode;

public abstract class AsNStatement extends AsNNode {

	public static AsNStatement cast(Statement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNStatement node = null;
		if (s instanceof CompoundStatement) {
			node = AsNCompoundStatement.cast((CompoundStatement) s, r, st);
		}
		else if (s instanceof Return) {
			node = AsNReturn.cast((Return) s, r, st); 
		}
		else if (s instanceof Declaration) {
			node = AsNDeclaration.cast((Declaration) s, r, st);
		}
		else if (s instanceof Assignment) {
			node = AsNAssignment.cast((Assignment) s, r, st); 
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No cast available for " + s.getClass().getName());
	
		s.castedNode = node;
		return node;
	}
	
}
