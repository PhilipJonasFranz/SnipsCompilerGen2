package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMEor;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitXor extends AsNBinaryExpression {

	public static AsNBitXor cast(BitXor b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitXor xor = new AsNBitXor();
		
		xor.generateLoaderCode(xor, b, r, map, st, (x, y) -> x << y, 
				new ASMEor(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
			
		return xor;
	}
	
}
