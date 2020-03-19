package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.ASM.Data.ASMPopStack;
import Imm.ASM.Data.ASMPushStack;
import Imm.ASM.Processing.ASMAdd;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNExpression;

public class AsNAdd extends AsNExpression {

	public AsNAdd() {
		
	}
	
	public static AsNAdd cast(Add a, RegSet r) {
		AsNAdd add = new AsNAdd();
		
		/* Process left and right operand */
		add.instructions.addAll(AsNExpression.cast(a.left(), r).getInstructions());
		add.instructions.add(new ASMPushStack(new RegisterOperand(REGISTER.R0)));
		r.regs [0].free();
		
		add.instructions.addAll(AsNExpression.cast(a.right(), r).getInstructions());
		
		add.instructions.add(new ASMMove(new RegisterOperand(2), new RegisterOperand(0)));
		r.copy(0, 2);
		
		add.instructions.add(new ASMPopStack(new RegisterOperand(REGISTER.R1)));
		
		add.instructions.add(new ASMAdd(new RegisterOperand(REGISTER.R0), new RegisterOperand(REGISTER.R1), new RegisterOperand(REGISTER.R2)));
		r.regs [0].setExpression(a);
		r.regs [1].free();
		r.regs [2].free();
		
		return add;
	}
	
}
