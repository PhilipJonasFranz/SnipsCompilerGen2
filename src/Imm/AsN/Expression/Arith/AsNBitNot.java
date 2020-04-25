package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMvn;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNBitNot extends AsNUnaryExpression {

			/* --- METHODS --- */
	public static AsNBitNot cast(BitNot b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNBitNot not = new AsNBitNot();
		
		/* Clear only R0 */
		not.clearReg(r, st, 0);
		
		/* Load Operand */
		not.instructions.addAll(AsNExpression.cast(b.getOperand(), r, map, st).getInstructions());
	
		not.instructions.add(new ASMMvn(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
		
		return not;
	}
	
}
