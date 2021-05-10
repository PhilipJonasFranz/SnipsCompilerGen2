package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMEor;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNBitXor extends AsNNFoldExpression {

	public static AsNBitXor cast(BitXor b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitXor xor = new AsNBitXor();
		xor.pushOnCreatorStack(b);
		b.castedNode = xor;
		
		xor.evalExpression(xor, b, r, map, st, (x, y) -> x ^ y);
			
		xor.registerMetric();
		return xor;
	}
	
	public ASMInstruction buildInjector() {
		return new ASMEor(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
} 
