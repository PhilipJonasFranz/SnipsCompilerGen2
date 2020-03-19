package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.ASM.Processing.ASMAddition;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNExpression;

public class AsNAddition extends AsNExpression {

	public AsNAddition() {
		
	}
	
	public static AsNAddition cast(Add a, RegSet r) {
		AsNAddition add = new AsNAddition();
		
		/* Process left and right operand */
		add.instructions.addAll(AsNExpression.cast(a.left(), r).getInstructions());
		add.instructions.add(new ASMMove(new RegisterOperand(1), new RegisterOperand(0)));
		add.instructions.addAll(AsNExpression.cast(a.right(), r).getInstructions());
		add.instructions.add(new ASMMove(new RegisterOperand(2), new RegisterOperand(0)));
		
		add.instructions.add(new ASMAddition(new RegisterOperand(REGISTER.R0), new RegisterOperand(REGISTER.R1), new RegisterOperand(REGISTER.R2)));
		
		return add;
	}
	
}
