package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AsN.AsNNode;

public abstract class AsNStatement extends AsNNode {

	public static AsNStatement cast(Statement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNStatement node = null;
		
		/* Free operand registers */
		r.free(0, 1, 2);
		
		if (s instanceof CompoundStatement) {
			node = AsNCompoundStatement.cast((CompoundStatement) s, r, map, st);
		}
		else if (s instanceof FunctionCall) {
			node = AsNFunctionCall.cast((FunctionCall) s, r, map, st); 
		}
		else if (s instanceof ReturnStatement) {
			node = AsNReturn.cast((ReturnStatement) s, r, map, st); 
		}
		else if (s instanceof BreakStatement) {
			node = AsNBreak.cast((BreakStatement) s, r, map, st); 
		}
		else if (s instanceof ContinueStatement) {
			node = AsNContinue.cast((ContinueStatement) s, r, map, st); 
		}
		else if (s instanceof Declaration) {
			node = AsNDeclaration.cast((Declaration) s, r, map, st);
		}
		else if (s instanceof Assignment) {
			node = AsNAssignment.cast((Assignment) s, r, map, st); 
		}
		else if (s instanceof AssignWriteback) {
			node = AsNAssignWriteback.cast((AssignWriteback) s, r, map, st); 
		}
		else if (s instanceof SwitchStatement) {
			node = AsNSwitchStatement.cast((SwitchStatement) s, r, map, st); 
		}
		else if (s instanceof SignalStatement) {
			node = AsNSignalStatement.cast((SignalStatement) s, r, map, st); 
		}
		else if (s instanceof Comment) {
			node = AsNComment.cast((Comment) s, r, map, st); 
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No injection cast available for " + s.getClass().getName());
	
		s.castedNode = node;
		return node;
	}
	
}
