package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.WhileStatement;

public abstract class AsNConditionalCompoundStatement extends AsNCompoundStatement {

	public static AsNConditionalCompoundStatement cast(ConditionalCompoundStatement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNConditionalCompoundStatement node = null;
		
		if (s instanceof IfStatement) {
			node = AsNIfStatement.cast((IfStatement) s, r, st);
		}
		else if (s instanceof WhileStatement) {
			node = AsNWhileStatement.cast((WhileStatement) s, r, st);
		}
		else if (s instanceof ForStatement) {
			node = AsNForStatement.cast((ForStatement) s, r, st);
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No injection cast available for " + s.getClass().getName());	
	
		s.castedNode = node;
		return node;
	}
	
}
