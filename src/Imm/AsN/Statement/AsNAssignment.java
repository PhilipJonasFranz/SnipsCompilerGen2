package Imm.AsN.Statement;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.RegOperand;
import Imm.AST.Statement.Assignment;
import Imm.AsN.Expression.AsNExpression;

public class AsNAssignment extends AsNStatement {

	public AsNAssignment() {
		
	}

	public static AsNAssignment cast(Assignment a, RegSet r) throws CGEN_EXCEPTION {
		AsNAssignment ass = new AsNAssignment();
		
		ass.instructions.addAll(AsNExpression.cast(a.value, r).getInstructions());
		
		int free = r.findFree();
		if (free != -1) {
			ass.instructions.add(new ASMMove(new RegOperand(free), new RegOperand(0)));
			r.regs [free].setExpression(a.value);
		}
		else {
			throw new CGEN_EXCEPTION(a.getSource(), "RegStack Overflow!");
		}
		
		return ass;
	}
	
}
