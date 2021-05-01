package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.SizeOfType;
import Imm.AsN.AsNBody;

public class AsNSizeOfType extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNSizeOfType cast(SizeOfType sot, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNSizeOfType s = new AsNSizeOfType();
		s.pushOnCreatorStack();
		sot.castedNode = s;
		
		r.free(0);
		
		/* Move word size in target register via literal manager, makes sure values > 255 are handeled correctly */
		AsNBody.literalManager.loadValue(s, sot.sizeType.wordsize(), target);
		
		s.registerMetric();
		return s;
	}
	
} 
