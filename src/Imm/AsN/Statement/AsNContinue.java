package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AST.Statement.ContinueStatement;

public class AsNContinue extends AsNStatement {

	public static AsNContinue cast(ContinueStatement c, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNContinue con = new AsNContinue();
		
		/* Retrieve the jump label target from the super loop */
		ASMLabel target = ((AsNConditionalCompoundStatement) c.superLoop.castedNode).continueJump;
		
		/* Jump to the label */
		con.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(target)));
		
		return con;
	}
	
}
