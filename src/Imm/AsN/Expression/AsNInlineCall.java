package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.InlineCall;
import Imm.AsN.Statement.AsNFunctionCall;
import Imm.AsN.Statement.AsNSignalStatement;

public class AsNInlineCall extends AsNExpression {

			/* --- METHODS --- */
	public static AsNInlineCall cast(InlineCall ic, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInlineCall call = new AsNInlineCall();
		ic.castedNode = call;
		
		/* Function may be null when its an anonymous call */
		if (ic.anonTarget == null) {
			/* 
			 * When a function has provisos, the order cannot be checked.
			 * A indicator the order is incorrect is that the casted node is null at this point.
			 */
			if (ic.calledFunction.castedNode == null && !ic.calledFunction.isLambdaHead) {
				throw new SNIPS_EXC("Function " + ic.calledFunction.path.build() + " is undefined at this point, " + ic.getSource().getSourceMarker());
			}
		}
		
		AsNFunctionCall.call(ic.calledFunction, ic.anonTarget, ic.proviso, ic.parameters, ic, call, r, map, st);
		
		if (ic.anonTarget == null && ic.calledFunction.signals()) {
			/* Check if exception was thrown and jump to watchpoint */
			call.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(0)));
			AsNSignalStatement.injectWatchpointBranch(call, ic.watchpoint, new Cond(COND.NE));
		}
		
		return call;
	}
	
} 
