package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Statement.BreakStatement;

public class AsNBreakStatement extends AsNStatement {

	public static AsNBreakStatement cast(BreakStatement b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBreakStatement br = new AsNBreakStatement();
		br.pushOnCreatorStack();
		b.castedNode = br;
		
		/* Retrieve the jump label target from the super loop */
		ASMLabel target = ((AsNConditionalCompoundStatement) b.superLoop.castedNode).breakJump;
		
		/* Resets the stack. When declarations are made and a break statement is called, 
		 * the normal loop exit is not taken, and thus the stack resetting is skipped.
		 * By passing the parameter false the stack set and reg set is not changed, but the
		 * correct offsets are determined, and the stack reset operation is inserted */
		AsNCompoundStatement.popDeclarationScope(br, b.superLoop, r, st, false);
		
		/* Jump to the loop escape label */
		br.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(target)));
		
		br.registerMetric();
		return br;
	}
	
} 
