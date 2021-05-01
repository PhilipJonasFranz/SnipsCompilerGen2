package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMMvn;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNBitNot extends AsNUnaryExpression {

			/* ---< METHODS >--- */
	public static AsNBitNot cast(BitNot b, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNBitNot not = new AsNBitNot();
		not.pushOnCreatorStack();
		b.castedNode = not;
		
		/* Clear only R0 */
		not.clearReg(r, st, 0);
		
		/* Load Operand */
		not.instructions.addAll(AsNExpression.cast(b.getOperand(), r, map, st).getInstructions());
	
		not.instructions.add(new ASMMvn(new RegOp(REG.R0), new RegOp(REG.R0)));
		
		not.registerMetric();
		return not;
	}
	
} 
