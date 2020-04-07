package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitOr extends AsNBinaryExpression {

	public static AsNBitOr cast(BitOr b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNBitOr or = new AsNBitOr();
		
		or.generateLoaderCode(or, b, r, map, st, (x, y) -> x << y, 
				new ASMOrr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
		return or;
	}
	
}
