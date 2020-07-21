package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.ArrayInit;

public class AsNArrayInit extends AsNExpression {

			/* --- METHODS --- */
	public static AsNArrayInit cast(ArrayInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNArrayInit init = new AsNArrayInit();
		s.castedNode = init;
		
		r.free(0, 1, 2);
		
		AsNStructureInit.structureInit(init, s.elements, r, map, st);
		
		return init;
	}
	
}
