package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNLsl extends AsNBinaryExpression {

	public AsNLsl() {
		
	}
	
	public static AsNLsl cast(Lsl l, RegSet r) throws CGEN_EXCEPTION {
		AsNLsl lsl = new AsNLsl();
		
		if (l.left() instanceof Atom && l.right() instanceof Atom) {
			lsl.atomicPrecalc(l, (x, y) -> x << y);
		}
		else {
			if (l.left() instanceof Atom) {
				lsl.loadRight(l, 1, r);
				lsl.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(((INT) ((Atom) l.left()).type).value)));
			}
			else if (l.right() instanceof Atom) {
				lsl.loadLeft(l, 0, r);
				lsl.instructions.add(new ASMMove(new RegOperand(1), new ImmOperand(((INT) ((Atom) l.right()).type).value)));
			}
			else {
				lsl.instructions.addAll(AsNExpression.cast(l.left(), r).getInstructions());
				lsl.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.regs [0].free();
				
				lsl.instructions.addAll(AsNExpression.cast(l.right(), r).getInstructions());
				
				lsl.instructions.add(new ASMMove(new RegOperand(1), new RegOperand(0)));
				r.copy(0, 1);
				
				lsl.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			}
			
			lsl.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			r.regs [0].setExpression(l);
			r.regs [1].free();
		}
		
		return lsl;
	}
	
}
