package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Statement.OperatorStatement;
import Imm.AsN.Expression.AsNOperatorExpression;

public class AsNOperatorStatement extends AsNStatement {

	public static AsNOperatorStatement cast(OperatorStatement op, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNOperatorStatement op0 = new AsNOperatorStatement();
		op0.pushOnCreatorStack(op);
		op.castedNode = op0;
		
		op0.instructions.addAll(AsNOperatorExpression.cast(op.expression, r, map, st).getInstructions());
		
		op0.registerMetric();
		return op0;
	}
	
} 
