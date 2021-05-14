package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Imm.ASM.Util.REG;
import Imm.AST.Expression.IDOfExpression;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNIDOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNIDOfExpression cast(IDOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNIDOfExpression s = new AsNIDOfExpression();
		s.pushOnCreatorStack(soe);
		soe.castedNode = s;
		
		r.free(0);
		
		STRUCT struct = (STRUCT) soe.type;
		struct.getTypedef().loadSIDInReg(s, REG.R0, struct.proviso);
		
		s.registerMetric();
		return s;
	}
	
} 
