package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMAnd;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitAnd extends AsNBinaryExpression {

	public static AsNBitAnd cast(BitAnd b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNBitAnd and = new AsNBitAnd();
		
		and.generateLoaderCode(and, b, r, map, st, (x, y) -> x << y, 
				new ASMAnd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
		return and;
	}
	
}
