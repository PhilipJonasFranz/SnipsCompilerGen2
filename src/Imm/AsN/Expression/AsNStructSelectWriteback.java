package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AsN.Statement.AsNAssignWriteback;

public class AsNStructSelectWriteback extends AsNExpression {
	
			/* ---< METHODS >--- */
	public static AsNStructSelectWriteback cast(StructSelectWriteback s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNStructSelectWriteback sel = new AsNStructSelectWriteback();
		sel.pushOnCreatorStack(s);
		s.castedNode = sel;
		
		AsNAssignWriteback.injectWriteback(sel, s, r, map, st, true);
		
		sel.registerMetric();
		return sel;
	}
	
} 
