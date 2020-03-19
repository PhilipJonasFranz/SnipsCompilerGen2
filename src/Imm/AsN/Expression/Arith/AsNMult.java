package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.ImmOperand;
import Imm.ASM.Util.RegOperand;
import Imm.ASM.Util.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNMult extends AsNExpression {

	public AsNMult() {
		
	}
	
	public static AsNMult cast(Mul m, RegSet r) throws CGEN_EXCEPTION {
		AsNMult mul = new AsNMult();
		
		/* Process left and right operand */
		
		/* Both operands are immediates, precalculate */
		if (m.left() instanceof Atom && m.right() instanceof Atom) {
			Atom l0 = (Atom) m.left(), r0 = (Atom) m.right();
			if (l0.type instanceof INT && r0.type instanceof INT) {
				INT i0 = (INT) l0.type, i1 = (INT) r0.type;
				mul.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(i0.value * i1.value)));
			}
		}
		else {
			mul.instructions.addAll(AsNExpression.cast(m.left(), r).getInstructions());
			mul.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.regs [0].free();
			
			mul.instructions.addAll(AsNExpression.cast(m.right(), r).getInstructions());
			
			mul.instructions.add(new ASMMove(new RegOperand(2), new RegOperand(0)));
			r.copy(0, 2);
			
			mul.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			
			mul.instructions.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			r.regs [0].setExpression(m);
			r.regs [1].free();
			r.regs [2].free();
		}
		
		return mul;
	}
	
}
