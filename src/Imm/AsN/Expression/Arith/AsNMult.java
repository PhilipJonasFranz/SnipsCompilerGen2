package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.ASM.Data.ASMPopStack;
import Imm.ASM.Data.ASMPushStack;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.ASMMult;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNExpression;

public class AsNMult extends AsNExpression {

	public AsNMult() {
		
	}
	
	public static AsNMult cast(Mul m, RegSet r) {
		AsNMult mul = new AsNMult();
		
		/* Process left and right operand */
		mul.instructions.addAll(AsNExpression.cast(m.left(), r).getInstructions());
		mul.instructions.add(new ASMPushStack(new RegisterOperand(REGISTER.R0)));
		r.regs [0].free();
		
		mul.instructions.addAll(AsNExpression.cast(m.right(), r).getInstructions());
		
		mul.instructions.add(new ASMMove(new RegisterOperand(2), new RegisterOperand(0)));
		r.copy(0, 2);
		
		mul.instructions.add(new ASMPopStack(new RegisterOperand(REGISTER.R1)));
		
		mul.instructions.add(new ASMMult(new RegisterOperand(REGISTER.R0), new RegisterOperand(REGISTER.R1), new RegisterOperand(REGISTER.R2)));
		r.regs [0].setExpression(m);
		r.regs [1].free();
		r.regs [2].free();
		
		return mul;
	}
	
}
