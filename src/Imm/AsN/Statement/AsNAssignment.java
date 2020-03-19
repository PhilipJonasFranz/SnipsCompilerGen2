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
		
		/* Process value */
		ass.instructions.addAll(AsNExpression.cast(a.value, r).getInstructions());
		
		/* Declaration already loaded, just move value into register */
		if (r.declarationLoaded(a.origin)) {
			int reg = r.declarationRegLocation(a.origin);
			ass.instructions.add(new ASMMove(new RegOperand(reg), new RegOperand(0)));
		}
		/* Not loaded, store value to stack at save position */
		else {
			throw new CGEN_EXCEPTION(a.getSource(), "Assign origin not loaded!");
		}
		
		return ass;
	}
	
}
