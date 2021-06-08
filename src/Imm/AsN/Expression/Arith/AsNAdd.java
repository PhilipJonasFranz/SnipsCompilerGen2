package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVAdd;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNAdd extends AsNNFoldExpression {

			/* ---< METHODS >--- */
	public static AsNAdd cast(Add a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAdd add = new AsNAdd().pushCreatorStack(a);

		add.evalExpression(add, a, r, map, st);

		return add.popCreatorStack();
	}
	
	public ASMInstruction buildInjector() {
		return new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2));
	}

	public ASMInstruction buildVInjector() {
		return new ASMVAdd(new VRegOp(REG.S0), new VRegOp(REG.S1), new VRegOp(REG.S2));
	}
	
} 
