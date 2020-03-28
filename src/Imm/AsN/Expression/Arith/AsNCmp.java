package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMCmp;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AsN.Expression.AsNBinaryExpression;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNCmp extends AsNBinaryExpression {

	public COND trueC, neg;
	
	public AsNCmp() {
		/**
		 * Compare both operands based on the set Comparator. Move #1 in into R0 if the
		 * expression is true, #0 if not.
		 */
	}
	
	public static AsNCmp cast(Compare c, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNCmp cmp = new AsNCmp();
		
		/* Clear only R0, R1 since R2 is not needed */
		cmp.clearReg(r, 0);
		cmp.clearReg(r, 1);
		
		if (c.right() instanceof Atom) {
			cmp.instructions.addAll(AsNExpression.cast(c.left(), r, st).getInstructions());
			cmp.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(((INT) ((Atom) c.right()).type).value)));
		}
		else {
			cmp.instructions.addAll(AsNExpression.cast(c.right(), r, st).getInstructions());
			cmp.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.free(0);
			
			cmp.instructions.addAll(AsNExpression.cast(c.left(), r, st).getInstructions());
			
			cmp.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			
			cmp.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
	
		cmp.trueC = cmp.toCondition(c.comparator);
		cmp.neg = cmp.negate(cmp.trueC);
		
		/* Move #1 into R0 when condition is true with comparator of c */
		cmp.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(cmp.trueC)));
		
		/* Move #0 into R0 when condition is false with negated operator of c */
		cmp.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(cmp.neg)));
	
		r.free(0, 1);
		
		return cmp;
	}
	
	protected COND toCondition(COMPARATOR c) {
		if (c == COMPARATOR.EQUAL) return COND.EQ;
		else if (c == COMPARATOR.NOT_EQUAL) return COND.NE;
		else if (c == COMPARATOR.GREATER_SAME) return COND.GE;
		else if (c == COMPARATOR.GREATER_THAN) return COND.GT;
		else if (c == COMPARATOR.LESS_SAME) return COND.LE;
		else if (c == COMPARATOR.LESS_THAN) return COND.LT;
		return null;
	}
	
	protected COND negate(COND c) {
		if (c == COND.EQ) return COND.NE;
		if (c == COND.NE) return COND.EQ;
		if (c == COND.GE) return COND.LT;
		if (c == COND.GT) return COND.LE;
		if (c == COND.LE) return COND.GT;
		if (c == COND.LT) return COND.GE;
		return null;
	}
	
}
