package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.CTX_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.FunctionCall;
import Imm.AsN.AsNFunction;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.TYPE;
import Util.Pair;

public class AsNFunctionCall extends AsNStatement {

			/* --- METHODS --- */
	public static AsNFunctionCall cast(FunctionCall fc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNFunctionCall call = new AsNFunctionCall();
		fc.castedNode = call;
		
		if (fc.anonTarget == null) {
			/* 
			 * When a function has provisos, the order cannot be checked.
			 * A indicator the order is incorrect is that the casted node is null at this point.
			 */
			if (fc.calledFunction.castedNode == null) {
				throw new SNIPS_EXC("Function " + fc.calledFunction.path.build() + " is undefined at this point, " + fc.getSource().getSourceMarker());
			}
		}
		
		call(fc.calledFunction, fc.anonTarget, false, fc.proviso, fc.parameters, fc, call, r, map, st);
		
		if (fc.anonTarget == null && fc.calledFunction.signals) {
			/* Check if exception was thrown and jump to watchpoint */
			call.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(0)));
			AsNSignalStatement.injectWatchpointBranch(call, fc.watchpoint, new Cond(COND.NE));
		}
		
		call.freeDecs(r, fc);
		return call;
	}
	
	public static List<Pair<Expression, Integer>> getDefaultMapping(List<Expression> params) {
		int r = 0;
		List<Pair<Expression, Integer>> mapping = new ArrayList();
		
		for (Expression e : params) {
			//dec.print(0, true);
			int wordSize = e.getType().wordsize();
			if (wordSize == 1 && r < 3) {
				/* Load in register */
				mapping.add(new Pair(e, r));
				r++;
			}
			else 
				/* Load in stack */
				mapping.add(new Pair(e, -1));
		}
		return mapping;
	}
	
	public static void call(Function f, Declaration anonCall, boolean inlineCall, List<TYPE> provisos, List<Expression> parameters, SyntaxElement callee, AsNNode call, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Clear the operand regs */
		r.free(0, 1, 2);
		
		if (f != null) {
			try {
				f.setContext(provisos);
			} catch (CTX_EXC e) {
				e.printStackTrace();
			}
		}
		
		int regMapping = 0;
		
		List<Integer> sMap = new ArrayList();
		
		/* Extract mapping locations from different mappings */
		if (f == null || (f != null && f.isLambdaHead)) {
			/* Load default mapping */
			List<Pair<Expression, Integer>> mapping = getDefaultMapping(parameters);
			mapping.stream().forEach(x -> sMap.add(x.second));
		}
		else {
			List<Pair<Declaration, Integer>> mapping = ((AsNFunction) f.castedNode).getParameterMapping();
			mapping.stream().forEach(x -> sMap.add(x.second));
		}
		
		/* Load Parameters in the Stack */
		for (int i = 0; i < sMap.size(); i++) {
			if (sMap.get(i) == -1) {
				/*
				 * At this point, special stack set handling is needed. The casted parameter can push dummies
				 * on the stack. Since these are function parameters, the called function will take care of
				 * them. We reset the stack set for the compile time here already.
				 */
				int s = st.getStack().size();
				
				call.instructions.addAll(AsNExpression.cast(parameters.get(i), r, map, st).getInstructions());
				
				/* Push Parameter in R0 on the stack */
				if (parameters.get(i).getType().wordsize() == 1) {
					call.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				}
				
				while (st.getStack().size() != s) st.pop();
				r.getReg(0).free();
			}
		}
		
		/* Load Parameters in the registers */
		for (int i = sMap.size() - 1; i >= 0; i--) {
			if (sMap.get(i) != -1) {
				regMapping++;
				call.instructions.addAll(AsNExpression.cast(parameters.get(i), r, map, st).getInstructions());
				
				/* Leave First Parameter directley in R0 */
				if (sMap.get(i) > 0) {
					call.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				}
				r.getReg(0).free();
			}
		}
		
		/* Pop Parameters on the stack into the correct registers, 
		 * 		Parameter for R0 is already located in reg */
		if (regMapping >= 3) {
			call.instructions.add(new ASMPopStack(new RegOp(REG.R1), new RegOp(REG.R2)));
		}
		else if (regMapping == 2) {
			call.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
		}
		
		if ((f != null && f.isLambdaHead) || anonCall != null) {
			if (anonCall != null) {
				if (r.declarationLoaded(anonCall)) {
					int loc = r.declarationRegLocation(anonCall);
					
					/* Manual linking */
					call.instructions.add(new ASMAdd(new RegOp(REG.LR), new RegOp(REG.PC), new ImmOp(8)));
					
					/* Move address of function into pc */
					call.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(loc)));
				}
				else throw new SNIPS_EXC("Anon call loader not implemented");
			}
			else {
				if (r.declarationLoaded(f.lambdaDeclaration)) {
					int loc = r.declarationRegLocation(f.lambdaDeclaration);
					
					/* Manual linking */
					call.instructions.add(new ASMAdd(new RegOp(REG.LR), new RegOp(REG.PC), new ImmOp(8)));
					
					/* Move address of function into pc */
					call.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(loc)));
				}
				else throw new SNIPS_EXC("Lambda Declaration loader not implemented");
			}
		}
		else {
			/* Branch to function */
			String target = f.path.build() + f.manager.getPostfix(provisos);
			
			ASMLabel functionLabel = new ASMLabel(target);
			
			ASMBranch branch = new ASMBranch(BRANCH_TYPE.BL, new LabelOp(functionLabel));
			branch.comment = new ASMComment("Call " + f.path.build());
			call.instructions.add(branch);
		}
		
		if (f != null) {
			/* 
			 * Push dummy values on the stack for the stack return value, but only if 
			 * there is a data target.
			 */
			if (f.getReturnType().wordsize() > 1) {
				if (inlineCall) {
					for (int i = 0; i < f.getReturnType().wordsize(); i++) {
						st.push(REG.R0);
					}
				}
				else {
					/* No data target, reset stack */
					call.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(f.getReturnType().wordsize() * 4)));
				}
			}
		}
		else {
			if (callee instanceof InlineCall) {
				InlineCall ic = (InlineCall) callee;
				
				/* 
				 * Anonymous inline call uses implicit type, push dummy values for this type.
				 */
				if (ic.getType().wordsize() > 1) {
					for (int i = 0; i < ic.getType().wordsize(); i++) {
						st.push(REG.R0);
					}
				}
			}
			else {
				/* Resets the stack by setting the SP to FP - (Frame Size * 4). */
				ASMSub sub = new ASMSub(new RegOp(REG.SP), new RegOp(REG.FP), new ImmOp(st.getFrameSize() * 4));
				sub.comment = new ASMComment("Reset the stack after anonymous call");
				
				call.instructions.add(sub);
			}
		}
		
		if (parameters.size() > 0) call.instructions.get(0).comment = new ASMComment("Load parameters");
	}
	
}
