package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Statement.ContinueStatement;

public class AsNContinue extends AsNStatement {

	public static AsNContinue cast(ContinueStatement c, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNContinue con = new AsNContinue();
		
		/* Retrieve the jump label target from the super loop */
		ASMLabel target = ((AsNConditionalCompoundStatement) c.superLoop.castedNode).continueJump;
		
		/* Resets the stack. When declarations are made and a continue statement is called, 
		 * the normal loop reset is skipped, which will cause declarations to pile on the stack.
		 * By passing the parameter false the stack set and reg set is not changed, but the
		 * correct offsets are determined. */
		AsNCompoundStatement.popDeclarationScope(con, c.superLoop, r, st, false);
		
		/* Jump to the label */
		con.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(target)));
		
		return con;
	}
	
}
