package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Logic.ASMCompare;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNCompare extends AsNBinaryExpression {

	public AsNCompare() {
		
	}
	
	public static AsNCompare cast(Compare c, RegSet r) throws CGEN_EXCEPTION {
		AsNCompare cmp = new AsNCompare();
	
		if (c.right() instanceof Atom) {
			cmp.instructions.addAll(AsNExpression.cast(c.left(), r).getInstructions());
			cmp.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new ImmOperand(((INT) ((Atom) c.right).type).value)));
		}
		else {
			cmp.instructions.addAll(AsNExpression.cast(c.left(), r).getInstructions());
			cmp.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.regs [0].free();
			
			cmp.instructions.addAll(AsNExpression.cast(c.right(), r).getInstructions());
			
			cmp.instructions.add(new ASMMove(new RegOperand(1), new RegOperand(0)));
			r.copy(0, 1);
			
			cmp.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			
			cmp.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
	
		r.regs [0].setExpression(c);
		r.regs [1].free();
		
		return cmp;
	}
	
}
