package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Boolean.Not;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNUnaryExpression;

public class AsNNot extends AsNUnaryExpression {

			/* ---< METHODS >--- */
	public static AsNNot cast(Not n, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNNot not = new AsNNot();
		not.pushOnCreatorStack();
		n.castedNode = not;
		
		/* Clear only R0 */
		r.free(0);
		
		not.instructions.addAll(AsNExpression.cast(n.getOperand(), r, map, st).getInstructions());
		not.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
	
		/* Move #1 into R0 when condition is false */
		not.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.EQ));
		
		/* Move #0 into R0 when condition is true */
		not.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), COND.NE));
		
		r.free(0);
		
		not.registerMetric();
		return not;
	}
	
} 
