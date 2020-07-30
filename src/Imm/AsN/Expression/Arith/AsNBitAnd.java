package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMAnd;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitAnd extends AsNBinaryExpression {

	public static AsNBitAnd cast(BitAnd b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitAnd and = new AsNBitAnd();
		
		and.generateLoaderCode(and, b, r, map, st, (x, y) -> x << y, 
				new ASMAnd(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
			
		return and;
	}
	
} 
