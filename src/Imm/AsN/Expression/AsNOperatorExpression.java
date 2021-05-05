package Imm.AsN.Expression;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.OperatorExpression;

public class AsNOperatorExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNOperatorExpression cast(OperatorExpression op, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNOperatorExpression op0 = new AsNOperatorExpression();
		op0.pushOnCreatorStack(op);
		op.castedNode = op0;
		
		if (op.calledFunction == null) 
			op0.instructions.addAll(AsNExpression.cast(op.actualExpression, r, map, st).getInstructions());
		else {
			List<Expression> operands = op.extractOperands();
			
			InlineCall ic = new InlineCall(op.calledFunction.path.clone(), new ArrayList(), operands, op.getSource());
			ic.calledFunction = op.calledFunction;
			ic.proviso = op.provisoTypes;
			
			op0.instructions.addAll(AsNInlineCall.cast(ic, r, map, st).getInstructions());
		}
		
		op0.registerMetric();
		return op0;
	}
	
} 
