package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.ArrayInit;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNArrayInit extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNArrayInit cast(ArrayInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNArrayInit init = new AsNArrayInit();
		init.pushOnCreatorStack(s);
		s.castedNode = init;
		
		r.free(0, 1, 2);
		
		ARRAY arr = (ARRAY) s.getType();
		
		AsNStructureInit.structureInit(init, s.elements, null, true, false, r, map, st, arr.elementType.isFloat());
		
		init.registerMetric();
		return init;
	}
	
} 
