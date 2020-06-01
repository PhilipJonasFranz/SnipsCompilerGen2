package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Or;
import Imm.AsN.Expression.AsNBinaryExpression;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNOr extends AsNBinaryExpression {

			/* --- METHODS --- */
	public static AsNOr cast(Or o, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNOr or = new AsNOr();
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (o.getLeft() instanceof Atom && o.getRight() instanceof Atom) {
			int value0 = ((INT) ((Atom) o.getLeft()).getType()).value;
			int value1 = ((INT) ((Atom) o.getRight()).getType()).value;
			or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand((value0 != 0 || value1 != 0)? 1 : 0)));
		}
		else if (o.getLeft() instanceof Atom) {
			int value = ((INT) ((Atom) o.getLeft()).getType()).value;
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.getRight(), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else if (o.getRight() instanceof Atom) {
			int value = ((INT) ((Atom) o.getRight()).getType()).value;
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.getLeft(), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else {
			/* Load Operands */
			or.generatePrimitiveLoaderCode(or, o, r, map, st, 0, 1);
			
			ASMOrr orr = new ASMOrr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1));
			orr.updateConditionField = true;
			or.instructions.add(orr);
			
			or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.EQ)));
			
			return or;
		}
		
		return or;
	}

}
