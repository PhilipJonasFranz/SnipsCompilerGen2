package Imm.AsN.Statement;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Return;
import Imm.AsN.AsNNode;

public abstract class AsNStatement extends AsNNode {

	public AsNStatement() {
		
	}
	
	public static AsNStatement cast(SyntaxElement s, RegSet r) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		if (s instanceof Return) {
			return AsNReturn.cast((Return) s, r); 
		}
		else if (s instanceof Declaration) {
			return AsNDeclaration.cast((Declaration) s, r);
		}
		else if (s instanceof Assignment) {
			return AsNAssignment.cast((Assignment) s, r); 
		}
		else if (s instanceof IfStatement) {
			return AsNIfStatement.cast((IfStatement) s, r);
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No cast available for " + s.getClass().getName());
	}
	
}
