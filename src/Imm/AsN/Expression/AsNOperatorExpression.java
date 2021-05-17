package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.OperatorExpression;

import java.util.ArrayList;

public class AsNOperatorExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNOperatorExpression cast(OperatorExpression op, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNOperatorExpression op0 = new AsNOperatorExpression();
		op0.pushOnCreatorStack(op);
		op.castedNode = op0;
		
		if (op.calledFunction == null)
			/* Not an operator expression, cast the original interpretation of the expression */
			op0.instructions.addAll(AsNExpression.cast(op.actualExpression, r, map, st).getInstructions());
		else {
			/* Is operator, cast inline call to operator function */
			InlineCall ic = new InlineCall(op.calledFunction.path.clone(), new ArrayList(), op.extractOperands(), op.getSource());
			ic.calledFunction = op.calledFunction;
			ic.proviso = op.provisoTypes;
			
			op0.instructions.addAll(AsNInlineCall.cast(ic, r, map, st).getInstructions());
		}
		
		op0.registerMetric();
		return op0;
	}
	
} 
