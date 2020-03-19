package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNMult extends AsNBinaryExpression {

	public AsNMult() {
		
	}
	
	public static AsNMult cast(Mul m, RegSet r) throws CGEN_EXCEPTION {
		AsNMult mul = new AsNMult();
		
		if (m.left() instanceof Atom && m.right() instanceof Atom) {
			mul.atomicPrecalc(m, (x, y) -> x * y);
		}
		else {
			if (m.left() instanceof Atom) {
				mul.loadRight(m, 2, r);
				mul.instructions.add(new ASMMove(new RegOperand(1), new ImmOperand(((INT) ((Atom) m.left()).type).value)));
			}
			else if (m.right() instanceof Atom) {
				mul.loadLeft(m, 1, r);
				mul.instructions.add(new ASMMove(new RegOperand(2), new ImmOperand(((INT) ((Atom) m.right()).type).value)));
			}
			else {
				mul.instructions.addAll(AsNExpression.cast(m.left(), r).getInstructions());
				mul.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.regs [0].free();
				
				mul.instructions.addAll(AsNExpression.cast(m.right(), r).getInstructions());
				
				mul.instructions.add(new ASMMove(new RegOperand(2), new RegOperand(0)));
				r.copy(0, 2);
				
				mul.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			}
			mul.instructions.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			r.regs [0].setExpression(m);
			r.regs [1].free();
			r.regs [2].free();
		}
		
		return mul;
	}
	
}
