package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.ReturnStatement;
import Imm.AsN.Expression.AsNExpression;

public class AsNReturn extends AsNStatement {

	public static AsNReturn cast(ReturnStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNReturn ret = new AsNReturn();
		
		if (s.value != null) {
			ret.instructions.addAll(AsNExpression.cast(s.value, r, map, st).getInstructions());
			if (!ret.instructions.isEmpty()) ret.instructions.get(0).comment = new ASMComment("Evaluate Expression");
		}
		
		ret.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOp(REG.LR)));
		
		ret.freeDecs(r, s);
		return ret;
	}
	
}
