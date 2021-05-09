package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNBitOr extends AsNNFoldExpression {

	public static AsNBitOr cast(BitOr b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitOr or = new AsNBitOr();
		or.pushOnCreatorStack(b);
		b.castedNode = or;
		
		or.evalExpression(or, b, r, map, st, (x, y) -> x | y);
			
		or.registerMetric();
		return or;
	}

	public ASMInstruction buildInjector() {
		return new ASMOrr(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
	public ASMInstruction buildVInjector() {
		throw new SNIPS_EXC("No VFP injector available for 'BitOr'!");
	}
	
} 
