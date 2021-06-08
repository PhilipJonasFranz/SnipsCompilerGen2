package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNLsl extends AsNNFoldExpression {

	public static AsNLsl cast(Lsl l, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNLsl lsl = new AsNLsl().pushCreatorStack(l);

		lsl.evalExpression(lsl, l, r, map, st);

		return lsl.popCreatorStack();
	}
	
	public ASMInstruction buildInjector() {
		return new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
} 
