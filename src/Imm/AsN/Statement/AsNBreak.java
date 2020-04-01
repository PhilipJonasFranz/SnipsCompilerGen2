package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AST.Statement.BreakStatement;

public class AsNBreak extends AsNStatement {

	public static AsNBreak cast(BreakStatement b, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNBreak br = new AsNBreak();
		
		/* Retrieve the jump label target from the super loop */
		ASMLabel target = ((AsNConditionalCompoundStatement) b.superLoop.castedNode).breakJump;
		
		/* Jump to the label */
		br.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(target)));
		
		return br;
	}
	
}
