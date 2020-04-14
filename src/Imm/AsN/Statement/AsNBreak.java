package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AST.Statement.BreakStatement;

public class AsNBreak extends AsNStatement {

	public static AsNBreak cast(BreakStatement b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNBreak br = new AsNBreak();
		
		/* Retrieve the jump label target from the super loop */
		ASMLabel target = ((AsNConditionalCompoundStatement) b.superLoop.castedNode).breakJump;
		
		/* Resets the stack. When declarations are made and a break statement is called, 
		 * the normal loop exit is not taken, and thus the stack resetting is skipped.
		 * By passing the parameter false the stack set and reg set is not changed, but the
		 * correct offsets are determined. */
		AsNCompoundStatement.popDeclarationScope(br, b.superLoop, r, st, false);
		
		/* Jump to the label */
		br.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(target)));
		
		return br;
	}
	
}
