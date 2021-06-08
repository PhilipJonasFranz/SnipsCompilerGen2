package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AsN.AsNBody;

public class AsNSizeOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNSizeOfExpression cast(SizeOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNSizeOfExpression s = new AsNSizeOfExpression().pushCreatorStack(soe);
		r.free(0);
		
		/* Move word size in target register via literal manager, makes sure values > 255 are handeled correctly */
		AsNBody.literalManager.loadValue(s, soe.sizeType.wordsize(), target, false, soe.sizeType.wordsize() + "");

		return s.popCreatorStack();
	}
	
} 
