package Imm.AsN.Expression;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Statement.Declaration;
import Imm.AsN.AsNFunction;
import Util.Pair;

public class AsNInlineCall extends AsNExpression {

			/* --- METHODS --- */
	public static AsNInlineCall cast(InlineCall ic, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNInlineCall call = new AsNInlineCall();
		ic.castedNode = call;
		
		/* Clear the operand regs */
		call.clearReg(r, st, 0, 1, 2);
		
		/* Load Mapping */
		List<Pair<Declaration, Integer>> mapping = 
			((AsNFunction) ic.calledFunction.castedNode).parameterMapping;

		int stackMapping = 0;
		
		/* Load Parameters in the Stack */
		for (int i = 0; i < mapping.size(); i++) {
			Pair<Declaration, Integer> p = mapping.get(i);
			if (p.tpl_2() == -1) {
				stackMapping++;
				call.instructions.addAll(AsNExpression.cast(ic.parameters.get(i), r, map, st).getInstructions());
				if (ic.parameters.get(i).type.wordsize() == 1) call.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.getReg(0).free();
			}
		}
		
		int regMapping = 0;
		
		/* Load Parameters in the registers */
		for (int i = mapping.size() - 1; i >= 0; i--) {
			Pair<Declaration, Integer> p = mapping.get(i);
			if (p.tpl_2() != -1) {
				regMapping++;
				call.instructions.addAll(AsNExpression.cast(ic.parameters.get(i), r, map, st).getInstructions());
				
				/* Leave First Parameter directley in R0 */
				if (p.tpl_2() > 0) call.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.getReg(0).free();
			}
		}
		
		/* Pop Parameters on the stack into the correct registers, 
		 * 		Parameter for R0 is already located in reg */
		if (regMapping >= 3) 
			call.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		else if (regMapping == 2) 
			call.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));

		/* Branch to function */
		ASMLabel functionLabel = (ASMLabel) ic.calledFunction.castedNode.instructions.get(0);
		call.instructions.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOperand(functionLabel)));
		
		/* Shrink Stack if parameters were passed through it */
		if (stackMapping > 0) {
			int size = 0;
			for (Pair<Declaration, Integer> p  : mapping) {
				if (p.tpl_2() == -1) {
					size += p.tpl_1().type.wordsize();
				}
			}
			
			call.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(size * 4)));
		}
		
		return call;
	}
	
}
