package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNMult extends AsNBinaryExpression {

	public static AsNMult cast(Mul m, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNMult mul = new AsNMult();
		
		mul.generateLoaderCode(mul, m, r, map, st, (x, y) -> x * y, 
				new ASMMult(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
		
		return mul;
	}
	
} 
