package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNLsr extends AsNBinaryExpression {

	public static AsNLsr cast(Lsr l, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNLsr lsr = new AsNLsr();
		
		lsr.generateLoaderCode(lsr, l, r, map, st, (x, y) -> x >> y, 
				new ASMLsr(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
		
		return lsr;
	}
	
} 
