package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.And;
import Imm.AsN.Expression.AsNBinaryExpression;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNAnd extends AsNBinaryExpression {

			/* --- METHODS --- */
	public static AsNAnd cast(And a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNAnd and = new AsNAnd();
		
		/* Clear only R0, R1 since R2 is not needed */
		and.clearReg(r, st, 0, 1);
		
		if (a.getLeft() instanceof Atom && a.getRight() instanceof Atom) {
			int value0 = ((INT) ((Atom) a.getLeft()).type).value;
			int value1 = ((INT) ((Atom) a.getRight()).type).value;
			and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand((value0 == 0 || value1 == 0)? 0 : 1), new Cond(COND.NE)));
		}
		else if (a.getLeft() instanceof Atom) {
			int value = ((INT) ((Atom) a.getLeft()).type).value;
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.getRight(), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else if (a.getRight() instanceof Atom) {
			int value = ((INT) ((Atom) a.getRight()).type).value;
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.getLeft(), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else {
			/* Load Operands */
			and.generatePrimitiveLoaderCode(and, a, r, map, st, 0, 1);
			
			/* Perform and */
			ASMAdd and0 = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(0));
			and0.updateConditionField = true;
			and.instructions.add(and0);
			
			and.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new ImmOperand(1), new Cond(COND.NE)));
			
			and.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
			
			and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new Cond(COND.NE)));
			and.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.EQ)));
			
			return and;
		}
		
		return and;
	}
	
}
