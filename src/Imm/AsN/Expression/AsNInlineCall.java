package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.InlineCall;
import Imm.AsN.Statement.AsNFunctionCall;

public class AsNInlineCall extends AsNExpression {

			/* --- METHODS --- */
	public static AsNInlineCall cast(InlineCall ic, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNInlineCall call = new AsNInlineCall();
		ic.castedNode = call;
		
		AsNFunctionCall.call(ic.calledFunction, ic.caller, ic.proviso, ic.parameters, call, r, map, st);
		
		return call;
	}
	
}
