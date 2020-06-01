package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Boolean.Not;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNNot extends AsNUnaryExpression {

			/* --- METHODS --- */
	public static AsNNot cast(Not n, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNNot not = new AsNNot();
		
		/* Clear only R0 */
		r.free(0);
		
		not.instructions.addAll(AsNExpression.cast(n.getOperand(), r, map, st).getInstructions());
		not.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
	
		/* Move #1 into R0 when condition is false */
		not.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.EQ)));
		
		/* Move #0 into R0 when condition is true */
		not.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.NE)));
		
		r.free(0);
		
		return not;
	}
	
}
