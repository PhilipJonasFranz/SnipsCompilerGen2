package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.REG;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ReturnStatement;
import Imm.AsN.Expression.AsNExpression;

public class AsNReturnStatement extends AsNStatement {

	public static AsNReturnStatement cast(ReturnStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNReturnStatement ret = new AsNReturnStatement().pushCreatorStack(s);

		/* Cast all declaration destroy operations */
		for (Declaration d : s.volatileDecsToDestroy)
			ret.instructions.addAll(AsNStatement.cast(d.volatileDestruct, r, map, st).getInstructions());

		if (s.value != null) {
			ret.instructions.addAll(AsNExpression.cast(s.value, r, map, st).getInstructions());
			if (!ret.instructions.isEmpty()) ret.instructions.get(0).com("Evaluate Expression");
		}

		/*
		 * Create symbolic bx branch. This branch will be replaced with a
		 * simple b branch to the function exit, that will contain exception,
		 * stack handling, as well as takes care of restoring the registers.
		 */
		ret.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOp(REG.LR)));
		
		ret.freeDecs(r, s);
		return ret.popCreatorStack();
	}
	
} 
