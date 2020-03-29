package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Statement.Assignment;
import Imm.AsN.Expression.AsNExpression;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNAssignment assign = new AsNAssignment();
		
		/* Process value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, st).getInstructions());
		
		/* Declaration already loaded, just move value into register */
		if (r.declarationLoaded(a.origin)) {
			int reg = r.declarationRegLocation(a.origin);
			assign.instructions.add(new ASMMov(new RegOperand(reg), new RegOperand(0)));
		}
		/* Not loaded, store value to stack or into register */
		else {
			int free = r.findFree();
			
			if (free != -1) {
				if (a.value != null) {
					assign.instructions.addAll(AsNExpression.cast(a.value, r, st).getInstructions());
				}
				assign.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(0)));
				r.copy(0, free);
			}
			else {
				/* Store to stack */
				
				// TODO Implement Stack functionality
				throw new CGEN_EXCEPTION(a.getSource(), "Assign origin not loaded!");
			}
		}
		
		return assign;
	}
	
}
