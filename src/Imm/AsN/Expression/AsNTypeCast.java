package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.TypeCast;

public class AsNTypeCast extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTypeCast cast(TypeCast tc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNTypeCast t = new AsNTypeCast();
		t.pushOnCreatorStack();
		tc.castedNode = t;
		
		/* 
		 * Relay to capsuled expression for now, currently no datatype requires some kind of transformation,
		 * FLOAT -> INT, INT -> FLOAT in the future maybe.
		 */
		t.instructions.addAll(AsNExpression.cast(tc.expression, r, map, st).getInstructions());
		
		t.registerMetric();
		return t;
	}
	
} 
