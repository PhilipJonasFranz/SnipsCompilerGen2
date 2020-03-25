package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.ASMAdd;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;

public class AsNInlineCall extends AsNExpression {

	public AsNInlineCall() {
		
	}
	
	public static AsNInlineCall cast(InlineCall ic, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNInlineCall call = new AsNInlineCall();
		ic.castedNode = call;
		
		/* Move parameters in regs and/or stack */
		r.free(0, 1, 2);
		
		/* Inflate Stack */
		for (Expression e : ic.parameters) {
			call.instructions.addAll(AsNExpression.cast(e, r, st).getInstructions());
			call.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.getReg(0).free();
		}
		
		/* Branch to function */
		ASMLabel functionLabel = (ASMLabel) ic.calledFunction.castedNode.instructions.get(1);
		call.instructions.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOperand(functionLabel)));
		
		/* Shrink Stack */
		call.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(ic.parameters.size() * 4)));
		
		/* Update Register Set */
		r.getReg(0).setExpression(ic);
		
		return call;
	}
	
}
