package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNExpression;

public class AsNSub extends AsNBinaryExpression {

	public AsNSub() {
		
	}
	
	public static AsNSub cast(Sub s, RegSet r) throws CGEN_EXCEPTION {
		AsNSub sub = new AsNSub();
	
/* Process left and right operand */
		
		/* Both operands are immediates, precalculate */
		if (s.left() instanceof Atom) {
			sub.loadRight(s, 2, r);
			sub.loadLeft(s, 1, r);
		}
		else if (s.right() instanceof Atom) {
			sub.loadLeft(s, 1, r);
			sub.loadRight(s, 2, r);
		}
		else {
			sub.instructions.addAll(AsNExpression.cast(s.left(), r).getInstructions());
			sub.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.regs [0].free();
			
			sub.instructions.addAll(AsNExpression.cast(s.right(), r).getInstructions());
			
			sub.instructions.add(new ASMMove(new RegOperand(2), new RegOperand(0)));
			r.copy(0, 2);
			
			sub.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
		}
		
		sub.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		r.regs [0].setExpression(s);
		r.regs [1].free();
		r.regs [2].free();
		
		return sub;
	}
	
}
