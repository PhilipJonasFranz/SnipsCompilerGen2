package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.IDOfExpression;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNIDOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNIDOfExpression cast(IDOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNIDOfExpression s = new AsNIDOfExpression();
		s.pushOnCreatorStack();
		soe.castedNode = s;
		
		r.free(0);
		
		STRUCT struct = (STRUCT) soe.type;
		struct.getTypedef().loadSIDInReg(s, REG.R0, struct.proviso);
		
		s.registerMetric();
		return s;
	}
	
} 
