package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVMult;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNMul extends AsNNFoldExpression {

	public static AsNMul cast(Mul m, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNMul mul = new AsNMul().pushCreatorStack(m);

		mul.evalExpression(mul, m, r, map, st);

		return mul.popCreatorStack();
	}
	
	public ASMInstruction buildInjector() {
		return new ASMMult(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}
	
	public ASMInstruction buildVInjector() {
		return new ASMVMult(new VRegOp(REG.S0), new VRegOp(REG.S1), new VRegOp(REG.S2));
	}
	
} 
