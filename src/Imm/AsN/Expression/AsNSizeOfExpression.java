package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AsN.AsNBody;

public class AsNSizeOfExpression extends AsNExpression {

			/* --- METHODS --- */
	public static AsNSizeOfExpression cast(SizeOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNSizeOfExpression s = new AsNSizeOfExpression();
		soe.castedNode = s;
		
		s.clearReg(r, st, 0);
		
		/* Move word size in target register */
		AsNBody.literalManager.loadValue(s, soe.sizeType.wordsize(), target);
		
		return s;
	}
	
}
