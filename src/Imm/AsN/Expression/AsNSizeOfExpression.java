package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AsN.AsNBody;

public class AsNSizeOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNSizeOfExpression cast(SizeOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNSizeOfExpression s = new AsNSizeOfExpression();
		s.pushOnCreatorStack();
		soe.castedNode = s;
		
		r.free(0);
		
		/* Move word size in target register via literal manager, makes sure values > 255 are handeled correctly */
		AsNBody.literalManager.loadValue(s, soe.sizeType.wordsize(), target);
		
		s.registerMetric();
		return s;
	}
	
} 
