package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMRsb;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNUnaryMinus extends AsNUnaryExpression {

			/* --- METHODS --- */
	public static AsNUnaryMinus cast(UnaryMinus m, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNUnaryMinus minus = new AsNUnaryMinus();
		
		/* Clear only R0 */
		minus.clearReg(r, st, 0);
		
		/* Load Operand */
		minus.instructions.addAll(AsNExpression.cast(m.getOperand(), r, map, st).getInstructions());
	
		/* 0 - Operand */
		minus.instructions.add(new ASMRsb(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(0)));
		
		return minus;
	}
	
}
