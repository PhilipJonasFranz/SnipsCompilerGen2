package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNLsr extends AsNNFoldExpression {

	public static AsNLsr cast(Lsr l, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNLsr lsr = new AsNLsr();
		lsr.pushOnCreatorStack(l);
		l.castedNode = lsr;
		
		lsr.evalExpression(lsr, l, r, map, st, (x, y) -> x >> y);
		
		lsr.registerMetric();
		return lsr;
	}
	
	public ASMInstruction buildInjector() {
		return new ASMLsr(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
	public ASMInstruction buildVInjector() {
		throw new SNIPS_EXC("No VFP injector available for 'Lsr'!");
	}
	
} 
