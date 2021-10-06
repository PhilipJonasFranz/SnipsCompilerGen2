package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.InlineCall;
import Imm.AsN.Statement.AsNFunctionCall;
import Imm.AsN.Statement.AsNSignalStatement;
import Res.Const;

public class AsNInlineCall extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNInlineCall cast(InlineCall ic, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInlineCall call = new AsNInlineCall();
		call.pushOnCreatorStack(ic);
		ic.castedNode = call;
		
		/* Function may be null when its an anonymous call */
		if (ic.anonTarget == null) {
			/* 
			 * When a function has provisos, the order cannot be checked.
			 * A indicator the order is incorrect is that the casted node is null at this point.
			 */
			if (ic.calledFunction.castedNode == null && !ic.calledFunction.isLambdaHead && ic.calledFunction.definedInInterface == null) {
				throw new SNIPS_EXC(Const.FUNCTION_UNDEFINED_AT_THIS_POINT, ic.calledFunction.path, ic.getSource().getSourceMarker());
			}
		}
		
		AsNFunctionCall.call(ic.calledFunction, ic.anonTarget, ic.proviso, ic.parameters, ic, call, r, map, st);
		
		if (ic.anonTarget == null && ic.calledFunction.signals()) {
			/* Check if exception was thrown and jump to watchpoint */
			call.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(0)));
			AsNSignalStatement.injectWatchpointBranch(call, ic.watchpoint, COND.NE);
		}
		
		call.registerMetric();
		return call;
	}
	
} 
