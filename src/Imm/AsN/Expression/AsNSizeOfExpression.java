package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Expression.SizeOfExpression;

public class AsNSizeOfExpression extends AsNExpression {

			/* --- METHODS --- */
	public static AsNSizeOfExpression cast(SizeOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNSizeOfExpression s = new AsNSizeOfExpression();
		soe.castedNode = s;
		
		s.clearReg(r, st, 0);
		
		/* Move word size in target register */
		s.instructions.add(new ASMMov(new RegOperand(target), new ImmOperand(soe.sizeType.wordsize())));
		
		return s;
	}
	
}
