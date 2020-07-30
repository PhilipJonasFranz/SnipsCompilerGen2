package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNLsl extends AsNBinaryExpression {

	public static AsNLsl cast(Lsl l, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNLsl lsl = new AsNLsl();
		
		lsl.generateLoaderCode(lsl, l, r, map, st, (x, y) -> x << y, 
				new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
			
		return lsl;
	}
	
} 
