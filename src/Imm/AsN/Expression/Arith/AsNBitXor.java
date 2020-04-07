package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMEor;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitXor extends AsNBinaryExpression {

	public static AsNBitXor cast(BitXor b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNBitXor xor = new AsNBitXor();
		
		xor.generateLoaderCode(xor, b, r, map, st, (x, y) -> x << y, 
				new ASMEor(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
		return xor;
	}
	
}
