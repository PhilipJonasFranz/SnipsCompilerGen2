package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMRsb;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVNeg;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNUnaryMinus extends AsNUnaryExpression {

			/* ---< METHODS >--- */
	public static AsNUnaryMinus cast(UnaryMinus m, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNUnaryMinus minus = new AsNUnaryMinus().pushCreatorStack(m);

		/* Clear only R0 */
		minus.clearReg(r, st, false, 0);
		
		/* Load Operand */
		minus.instructions.addAll(AsNExpression.cast(m.getOperand(), r, map, st).getInstructions());
	
		/* 0 - Operand */
		if (m.getOperand().getType().isFloat()) 
			minus.instructions.add(new ASMVNeg(new VRegOp(REG.S0), new VRegOp(REG.S0)));
		else 
			minus.instructions.add(new ASMRsb(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(0)));

		return minus.popCreatorStack();
	}
	
} 
