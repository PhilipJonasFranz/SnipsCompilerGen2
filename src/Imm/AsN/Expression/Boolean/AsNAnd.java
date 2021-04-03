package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.And;
import Imm.AsN.Expression.AsNBinaryExpression;
import Imm.AsN.Expression.AsNExpression;

public class AsNAnd extends AsNBinaryExpression {

			/* ---< METHODS >--- */
	public static AsNAnd cast(And a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAnd and = new AsNAnd();
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (a.getLeft() instanceof Atom && a.getRight() instanceof Atom) {
			int value0 = a.getLeft().getType().toInt();
			int value1 = a.getRight().getType().toInt();
			
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp((value0 == 0 || value1 == 0)? 0 : 1)));
		}
		else if (a.getLeft() instanceof Atom) {
			int value = a.getLeft().getType().toInt();
			
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.getRight(), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
			}
		}
		else if (a.getRight() instanceof Atom) {
			int value = a.getRight().getType().toInt();
			
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.getLeft(), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
			}
		}
		else {
			/* Load Operands */
			and.generatePrimitiveLoaderCode(and, a, r, map, st, 0, 1);
			
			/* Perform and */
			ASMAdd and0 = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(0));
			and0.updateCondField();
			and.instructions.add(and0);
			
			and.instructions.add(new ASMMov(new RegOp(REG.R1), new ImmOp(1), new Cond(COND.NE)));
			
			and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.R1), new Cond(COND.NE)));
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.EQ)));
			
			return and;
		}
		
		return and;
	}
	
} 
