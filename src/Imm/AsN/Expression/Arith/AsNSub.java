package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVSub;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNSub extends AsNNFoldExpression {
	
	public static AsNSub cast(Sub s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNSub sub = new AsNSub().pushCreatorStack(s);

		sub.evalExpression(sub, s, r, map, st);

		return sub.popCreatorStack();
	}
	
	public ASMInstruction buildInjector() {
		return new ASMSub(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
	public ASMInstruction buildVInjector() {
		return new ASMVSub(new VRegOp(REG.S0), new VRegOp(REG.S1), new VRegOp(REG.S2));
	}
	
} 
