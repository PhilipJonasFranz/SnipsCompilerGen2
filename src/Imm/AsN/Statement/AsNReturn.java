package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Return;
import Imm.AsN.Expression.AsNExpression;

public class AsNReturn extends AsNStatement {

	public static AsNReturn cast(Return s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNReturn ret = new AsNReturn();
		
		ret.instructions.addAll(AsNExpression.cast(s.value, r, st).getInstructions());
		ret.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOperand(REGISTER.LR)));
		
		return ret;
	}
	
}
