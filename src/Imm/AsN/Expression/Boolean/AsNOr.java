package Imm.AsN.Expression.Boolean;

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
	public static AsNOr cast(Or o, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNOr or = new AsNOr();
		
		/* Clear only R0, R1 since R2 is not needed */
		or.clearReg(r, st, 0, 1);
		
		if (o.left() instanceof Atom && o.right() instanceof Atom) {
			int value0 = ((INT) ((Atom) o.left()).type).value;
			int value1 = ((INT) ((Atom) o.right()).type).value;
			or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand((value0 == 1 || value1 == 1)? 1 : 0), new Cond(COND.NE)));
		}
		else if (o.left() instanceof Atom) {
			int value = ((INT) ((Atom) o.left()).type).value;
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.right(), r, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else if (o.right() instanceof Atom) {
			int value = ((INT) ((Atom) o.right()).type).value;
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.left(), r, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
			}
		}
		else {
			/* Load Operands */
			or.generatePrimitiveLoaderCode(or, o, r, st, 0, 1);
			
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
