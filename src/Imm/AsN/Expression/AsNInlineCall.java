package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.InlineCall;
import Imm.AsN.Statement.AsNFunctionCall;
import Imm.AsN.Statement.AsNSignalStatement;

public class AsNInlineCall extends AsNExpression {

			/* --- METHODS --- */
	public static AsNInlineCall cast(InlineCall ic, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNInlineCall call = new AsNInlineCall();
		ic.castedNode = call;
		
		/* 
		 * When a function has provisos, the order cannot be checked.
		 * A indicator the order is incorrect is that the casted node is null at this point.
		 */
		if (ic.calledFunction.castedNode == null) {
			throw new SNIPS_EXCEPTION("Function " + ic.calledFunction.path.build() + " is undefined at this point, " + ic.getSource().getSourceMarker());
		}
		
		AsNFunctionCall.call(ic.calledFunction, true, ic.proviso, ic.parameters, call, r, map, st);
		
		if (ic.calledFunction.signals) {
			/* Check if exception was thrown and jump to watchpoint */
			call.instructions.add(new ASMCmp(new RegOperand(REGISTER.R12), new ImmOperand(0)));
			AsNSignalStatement.injectWatchpointBranch(call, ic.watchpoint, new Cond(COND.NE));
		}
		
		return call;
	}
	
}
