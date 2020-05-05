package Imm.AsN.Statement;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.CTX_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.FunctionCall;
import Imm.AsN.AsNFunction;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.TYPE;
import Util.Pair;

public class AsNFunctionCall extends AsNStatement {

			/* --- METHODS --- */
	public static AsNFunctionCall cast(FunctionCall fc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNFunctionCall call = new AsNFunctionCall();
		fc.castedNode = call;
		
		/* 
		 * When a function has provisos, the order cannot be checked.
		 * A indicator the order is incorrect is that the casted node is null at this point.
		 */
		if (fc.calledFunction.castedNode == null) {
			throw new SNIPS_EXCEPTION("Function " + fc.calledFunction.path.build() + " is undefined at this point, " + fc.getSource().getSourceMarker());
		}
		
		call(fc.calledFunction, fc.proviso, fc.parameters, call, r, map, st);
		
		return call;
	}
	
	public static void call(Function f, List<TYPE> provisos, List<Expression> parameters, AsNNode call, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Clear the operand regs */
		call.clearReg(r, st, 0, 1, 2);
		
		try {
			f.setContext(provisos);
		} catch (CTX_EXCEPTION e) {
			e.printStackTrace();
		}
		
		/* Reload mapping for context */
		List<Pair<Declaration, Integer>> mapping = 
			((AsNFunction) f.castedNode).getParameterMapping();

		int stackMapping = 0;
		
		/* Load Parameters in the Stack */
		for (int i = 0; i < mapping.size(); i++) {
			Pair<Declaration, Integer> p = mapping.get(i);
			if (p.getSecond() == -1) {
				stackMapping++;
				call.instructions.addAll(AsNExpression.cast(parameters.get(i), r, map, st).getInstructions());
				if (parameters.get(i).getType().wordsize() == 1) {
					call.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				}
				r.getReg(0).free();
			}
		}
		
		int regMapping = 0;
		
		/* Load Parameters in the registers */
		for (int i = mapping.size() - 1; i >= 0; i--) {
			Pair<Declaration, Integer> p = mapping.get(i);
			if (p.getSecond() != -1) {
				regMapping++;
				call.instructions.addAll(AsNExpression.cast(parameters.get(i), r, map, st).getInstructions());
				
				/* Leave First Parameter directley in R0 */
				if (p.getSecond() > 0) {
					call.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				}
				r.getReg(0).free();
			}
		}
		
		/* Pop Parameters on the stack into the correct registers, 
		 * 		Parameter for R0 is already located in reg */
		if (regMapping >= 3) {
			call.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		}
		else if (regMapping == 2) {
			call.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
		}
		
		/* Branch to function */
		String target = f.path.build() + f.manager.getPostfix(provisos);
		
		ASMLabel functionLabel = new ASMLabel(target);
		
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.BL, new LabelOperand(functionLabel));
		branch.comment = new ASMComment("Call " + f.path.build());
		call.instructions.add(branch);
		
		/* Shrink Stack if parameters were passed through it */
		if (stackMapping > 0) {
			int size = 0;
			for (Pair<Declaration, Integer> p  : mapping) {
				if (p.getSecond() == -1) {
					size += p.getFirst().getType().wordsize();
				}
			}
			
			call.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(size * 4)));
		}
		
		if (!f.parameters.isEmpty()) call.instructions.get(0).comment = new ASMComment("Load parameters");
	}
	
}
