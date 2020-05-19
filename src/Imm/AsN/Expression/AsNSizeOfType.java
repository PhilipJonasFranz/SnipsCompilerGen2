package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.SizeOfType;
import Imm.AsN.AsNBody;

public class AsNSizeOfType extends AsNExpression {

			/* --- METHODS --- */
	public static AsNSizeOfType cast(SizeOfType sot, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNSizeOfType s = new AsNSizeOfType();
		sot.castedNode = s;
		
		s.clearReg(r, st, 0);
		
		/* Move word size in target register */
		AsNBody.literalManager.loadValue(s, sot.sizeType.wordsize(), target);
		
		return s;
	}
	
}
