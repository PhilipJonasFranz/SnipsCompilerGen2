package Imm.AsN.Statement;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Statement.Assignment;
import Imm.AsN.Expression.AsNExpression;

public class AsNAssignment extends AsNStatement {

	public AsNAssignment() {
		
	}

	public static AsNAssignment cast(Assignment a, RegSet r) throws CGEN_EXCEPTION {
		AsNAssignment ass = new AsNAssignment();
		
		ass.instructions.addAll(AsNExpression.cast(a.value, r).getInstructions());
		
		if (r.declarationLoaded(a.origin)) {
			int reg = r.declarationRegLocation(a.origin);
			ass.instructions.add(new ASMMove(new RegOperand(reg), new RegOperand(0)));
		}
		else {
			throw new CGEN_EXCEPTION(a.getSource(), "Assign origin not loaded!");
		}
		
		return ass;
	}
	
}
