package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMAnd;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNBitAnd extends AsNNFoldExpression {

	public static AsNBitAnd cast(BitAnd b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitAnd and = new AsNBitAnd();
		and.pushOnCreatorStack(b);
		b.castedNode = and;
		
		and.evalExpression(and, b, r, map, st, (x, y) -> x & y);
			
		and.registerMetric();
		return and;
	}
	
	public ASMInstruction buildInjector() {
		return new ASMAnd(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
} 
