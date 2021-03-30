package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
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
import Imm.AST.Expression.TempAtom;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AsN.AsNFunction;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Res.Const;
import Util.Pair;

public class AsNFunctionCall extends AsNStatement {

			/* ---< METHODS >--- */
	public static AsNFunctionCall cast(FunctionCall fc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNFunctionCall call = new AsNFunctionCall();
		fc.castedNode = call;
		
		if (fc.anonTarget == null) 
			/* 
			 * When a function has provisos, the order cannot be checked.
			 * A indicator the order is incorrect is that the casted node is null at this point.
			 */
			if (fc.calledFunction.castedNode == null && fc.calledFunction.definedInInterface == null) 
				throw new SNIPS_EXC(Const.FUNCTION_UNDEFINED_AT_THIS_POINT, fc.calledFunction.path.build(), fc.getSource().getSourceMarker());
		
		call(fc.calledFunction, fc.anonTarget, fc.proviso, fc.parameters, fc, call, r, map, st);
		
		if (fc.anonTarget == null && fc.calledFunction.signals()) {
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
			if (e.getType().wordsize() == 1 && r < 3 && !(e.getType() instanceof STRUCT)) {
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
	
	public static void call(Function f, Declaration anonCall, List<TYPE> provisos, List<Expression> parameters, SyntaxElement callee, AsNNode call, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Clear the operand regs */
		r.free(0, 1, 2);
		
		if (f != null) {
			try {
				f.setContext(provisos);
			} catch (CTEX_EXC e) {
				e.printStackTrace();
			}
		}
		
		int regMapping = 0;
		
		List<Integer> sMap = new ArrayList();
		
		/* Extract mapping locations from different mappings */
		if (f == null || (f != null && (f.isLambdaHead || f.definedInInterface != null))) {
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
				
				boolean placeholder = false;
				if (parameters.get(i) instanceof TempAtom) {
					TempAtom a = (TempAtom) parameters.get(i);
					if (a.getType().wordsize() > 1) placeholder = true;
				}
				
				/* Push Parameter in R0 on the stack, but only if parameter is not an atom placeholder that pushes itself on the stack */
				if (parameters.get(i).getType().wordsize() == 1 && !placeholder && !(parameters.get(i).getType() instanceof STRUCT)) 
					call.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				
				while (st.getStack().size() != s) st.pop();
				
				r.free(0);
			}
		}
		
		/* Load Parameters in the registers */
		for (int i = sMap.size() - 1; i >= 0; i--) {
			if (sMap.get(i) != -1) {
				regMapping++;
				call.instructions.addAll(AsNExpression.cast(parameters.get(i), r, map, st).getInstructions());
				
				/* Leave First Parameter directley in R0 */
				if (sMap.get(i) > 0) {
					ASMPushStack push = new ASMPushStack(new RegOp(REG.R0));
					
					/* Add Opt flag so optimizer does not clear it */
					push.optFlags.add(OPT_FLAG.STRUCT_INIT);
					
					call.instructions.add(push);
				}
				
				r.free(0);
			}
		}
		
		/* 
		 * Pop Parameters on the stack into the correct registers, 
		 * Parameter for R0 is already located in reg 
		 */
		if (regMapping >= 3) 
			call.instructions.add(new ASMPopStack(new RegOp(REG.R1), new RegOp(REG.R2)));
		else if (regMapping == 2) 
			call.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
		
		if (f != null && f.definedInInterface != null) {
			
			/*
			 * R0  = SID of struct w. mapping
			 * R10 = IID of interface w. mapping
			 * R12 = Address to resolver
			 * In Stack = Offset to Function, popped by table mapping
			 */
			
			InterfaceTypedef inter = f.definedInInterface;
			
			boolean found = false;
			int offset = 0;
			for (int i = 0; i < inter.functions.size(); i++) {
				if (Function.signatureMatch(inter.functions.get(i), f, false, true, false)) {
					offset = i * 4;
					found = true;
					break;
				}
			}
			
			/* Make sure the function was found */
			assert found : "Failed to locate function '" + f.path.build() + "'!";
			
			boolean nestedDeref = false;
			if (callee instanceof InlineCall)
				nestedDeref = ((InlineCall) callee).nestedDeref;
			
			/* Interface reference is a pointer, and call uses deref, need to load from pointer */
			if (nestedDeref) {
				call.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
				call.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
			}
			
			/* Load and push the function offset for later use */
			ASMMov offsetMov = new ASMMov(new RegOp(REG.R12), new ImmOp(offset));
			offsetMov.comment = new ASMComment("Offset to " + f.path.build());
			call.instructions.add(offsetMov);
			call.instructions.add(new ASMPushStack(new RegOp(REG.R12)));
			
			/* Load address of struct interface resolver */
			call.instructions.add(new ASMLsl(new RegOp(REG.R12), new RegOp(REG.R0), new ImmOp(2)));
			call.instructions.add(new ASMLdr(new RegOp(REG.R12), new RegOp(REG.R12)));
			call.instructions.add(new ASMLdr(new RegOp(REG.R12), new RegOp(REG.R12), new ImmOp(4)));
			
			/* Load IID of interface */
			inter.loadIIDInReg(call, REG.R10, provisos);
			
			/* Perform a system branch to resolver */
			call.instructions.add(new ASMAdd(new RegOp(REG.LR), new RegOp(REG.PC), new ImmOp(8)));
			call.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(REG.R12)));
		}
		else if ((f != null && f.isLambdaHead) || anonCall != null) {
			if (anonCall != null) {
				if (r.declarationLoaded(anonCall)) {
					int loc = r.declarationRegLocation(anonCall);
					
					/* Manual linking */
					call.instructions.add(new ASMAdd(new RegOp(REG.LR), new RegOp(REG.PC), new ImmOp(8)));
					
					/* Move address of function into pc */
					call.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(loc)));
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
			}
			else {
				if (r.declarationLoaded(f.lambdaDeclaration)) {
					int loc = r.declarationRegLocation(f.lambdaDeclaration);
					
					/* Manual linking */
					call.instructions.add(new ASMAdd(new RegOp(REG.LR), new RegOp(REG.PC), new ImmOp(8)));
					
					/* Move address of function into pc */
					call.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(loc)));
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
			}
		}
		else {
			/* Branch to function */
			String target = f.buildCallLabel(provisos);
			
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
				if (callee instanceof InlineCall) {
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
				int off = st.getFrameSize() * 4;
				
				/* Resets the stack by setting the SP to FP - (Frame Size * 4). */
				if (off != 0) {
					ASMSub sub = new ASMSub(new RegOp(REG.SP), new RegOp(REG.FP), new ImmOp(off));
					sub.comment = new ASMComment("Reset the stack after anonymous call");
					call.instructions.add(sub);
				}
				else {
					ASMMov mov = new ASMMov(new RegOp(REG.SP), new RegOp(REG.FP));
					mov.comment = new ASMComment("Reset the stack after anonymous call");
					call.instructions.add(mov);
				}
			}
		}
		
		if (parameters.size() > 0) call.instructions.get(0).comment = new ASMComment("Load parameters");
	}
	
} 
