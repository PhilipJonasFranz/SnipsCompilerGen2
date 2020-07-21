package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNSub extends AsNBinaryExpression {
	
	public static AsNSub cast(Sub s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNSub sub = new AsNSub();
	
		sub.generateLoaderCode(sub, s, r, map, st, (x, y) -> x - y, 
				new ASMSub(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
		
		return sub;
	}
	
}
