package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNLsr extends AsNBinaryExpression {

	public AsNLsr() {
		
	}
	
	public static AsNLsr cast(Lsr l, RegSet r) throws CGEN_EXCEPTION {
		AsNLsr lsr = new AsNLsr();
		
		if (l.left() instanceof Atom && l.right() instanceof Atom) {
			lsr.atomicPrecalc(l, (x, y) -> x >> y);
		}
		else {
			if (l.left() instanceof Atom) {
				lsr.loadRight(l, 1, r);
				lsr.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(((INT) ((Atom) l.left()).type).value)));
			}
			else if (l.right() instanceof Atom) {
				lsr.loadLeft(l, 0, r);
				lsr.instructions.add(new ASMMove(new RegOperand(1), new ImmOperand(((INT) ((Atom) l.right()).type).value)));
			}
			else {
				lsr.instructions.addAll(AsNExpression.cast(l.left(), r).getInstructions());
				lsr.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.regs [0].free();
				
				lsr.instructions.addAll(AsNExpression.cast(l.right(), r).getInstructions());
				
				lsr.instructions.add(new ASMMove(new RegOperand(1), new RegOperand(0)));
				r.copy(0, 1);
				
				lsr.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			}
			
			lsr.instructions.add(new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			r.regs [0].setExpression(l);
			r.regs [1].free();
		}
		
		return lsr;
	}
	
}
