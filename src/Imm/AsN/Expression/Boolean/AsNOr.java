package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Or;
import Imm.AsN.Expression.AsNBinaryExpression;
import Imm.AsN.Expression.AsNExpression;

public class AsNOr extends AsNBinaryExpression {

			/* ---< METHODS >--- */
	public static AsNOr cast(Or o, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNOr or = new AsNOr();
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (o.getLeft() instanceof Atom && o.getRight() instanceof Atom) {
			int value0 = o.getLeft().getType().toInt();
			int value1 = o.getRight().getType().toInt();
			
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp((value0 != 0 || value1 != 0)? 1 : 0)));
		}
		else if (o.getLeft() instanceof Atom) {
			int value = o.getLeft().getType().toInt();
			
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.getRight(), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
			}
		}
		else if (o.getRight() instanceof Atom) {
			int value = o.getRight().getType().toInt();
			
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.getLeft(), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
			}
		}
		else {
			/* Load Operands */
			or.generatePrimitiveLoaderCode(or, o, r, map, st, 0, 1);
			
			ASMOrr orr = new ASMOrr(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R1));
			orr.updateCondField();
			or.instructions.add(orr);
			
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.EQ)));
			
			return or;
		}
		
		return or;
	}

} 
