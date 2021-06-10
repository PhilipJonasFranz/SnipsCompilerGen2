package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.Lhs.AsNLhsId;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAssignment assign = new AsNAssignment().pushCreatorStack(a);

		if (a.assignArith != ASSIGN_ARITH.NONE)
			throw new SNIPS_EXC("Attempted to cast Assignment with assign arith, " + a.getSource().getSourceMarker());
		
		/* Compute value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
		if (!assign.instructions.isEmpty()) assign.instructions.get(0).com("Evaluate Expression");
		
		/* Store value at location specified by lhs */
		assign.instructions.addAll(AsNLhsId.cast(a.lhsId, r, map, st).getInstructions());
		
		assign.freeDecs(r, a);
		return assign.popCreatorStack();
	}
	
} 
