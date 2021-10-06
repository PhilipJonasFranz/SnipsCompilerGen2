package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AsN.Statement.AsNAssignWriteback;

public class AsNIDRefWriteback extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNIDRefWriteback cast(IDRefWriteback wb, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNIDRefWriteback w = new AsNIDRefWriteback();
		w.pushOnCreatorStack(wb);
		wb.castedNode = w;
		
		r.free(0, 1, 2);
		
		AsNAssignWriteback.injectWriteback(w, wb, r, map, st, true);
		
		w.registerMetric();
		return w;
	}
	
} 
