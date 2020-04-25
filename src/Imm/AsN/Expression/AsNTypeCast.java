package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.TypeCast;

public class AsNTypeCast extends AsNExpression {

			/* --- METHODS --- */
	public static AsNTypeCast cast(TypeCast tc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNTypeCast t = new AsNTypeCast();
		tc.castedNode = t;
		
		/* Relay to capsuled expression for now */
		t.instructions.addAll(AsNExpression.cast(tc.expression, r, map, st).getInstructions());
		
		return t;
	}
	
}
