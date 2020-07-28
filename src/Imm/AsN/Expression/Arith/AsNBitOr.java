package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNBitOr extends AsNBinaryExpression {

	public static AsNBitOr cast(BitOr b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitOr or = new AsNBitOr();
		
		or.generateLoaderCode(or, b, r, map, st, (x, y) -> x << y, 
				new ASMOrr(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
			
		return or;
	}
	
} 
