package Imm.AsN.Statement;

import CGen.RegSet;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Statement.Return;
import Imm.AsN.Expression.AsNExpression;

public class AsNReturn extends AsNStatement {
	
	public AsNReturn() {
		
	}

	public static AsNReturn cast(Return s, RegSet r) {
		AsNReturn ret = new AsNReturn();
		
		ret.instructions.addAll(AsNExpression.cast(s.value, r).getInstructions());
		ret.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegisterOperand(REGISTER.LR)));
		
		return ret;
	}
	
}
