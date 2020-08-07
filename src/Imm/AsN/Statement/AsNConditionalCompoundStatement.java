package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.WhileStatement;
import Res.Const;

public abstract class AsNConditionalCompoundStatement extends AsNCompoundStatement {

	public ASMLabel breakJump;
	
	public ASMLabel continueJump;
	
	public static AsNConditionalCompoundStatement cast(ConditionalCompoundStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNConditionalCompoundStatement node = null;
		
		if (s instanceof IfStatement) {
			node = AsNIfStatement.cast((IfStatement) s, r, map, st);
		}
		else if (s instanceof WhileStatement) {
			node = AsNWhileStatement.cast((WhileStatement) s, r, map, st);
		}
		else if (s instanceof DoWhileStatement) {
			node = AsNDoWhileStatement.cast((DoWhileStatement) s, r, map, st);
		}
		else if (s instanceof ForStatement) {
			node = AsNForStatement.cast((ForStatement) s, r, map, st);
		}
		else throw new CGEN_EXC(s.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, s.getClass().getName());	
	
		s.castedNode = node;
		return node;
	}
	
} 
