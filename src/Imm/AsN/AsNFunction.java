package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMMemOp;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNStatement;
import Util.Pair;

public class AsNFunction extends AsNNode {

	public List<Pair<Declaration, Integer>> parameterMapping;
	
	Function source;
	
	public AsNFunction() {
		
	}
	
	/**
	 * Casts given syntax element based on the given reg set to a asm function node. 
	 */
	public static AsNFunction cast(Function f, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNFunction func = new AsNFunction();
		f.castedNode = func;
		func.source = f;
		
		/* Setup Parameter Mapping */
		func.parameterMapping = func.getParameterMapping();
		
		/* Set Params in Registers */
		for (Pair<Declaration, Integer> p : func.parameterMapping) {
			/* Paramter is in stack, push in stackSet */
			if (p.tpl_2() == -1) {
				st.push(p.t0);
			}
			else {
				/* Load Declaration into register */
				r.getReg(p.tpl_2()).setDeclaration(p.tpl_1());
			}
		}
		
		/* Function Header and Entry Label */
		func.instructions.add(new ASMSectionAnnotation(SECTION.GLOBAL, func.source.functionName));
		func.instructions.add(new ASMLabel(func.source.functionName));
		
		/* Save FP and lr by default */
		ASMPushStack push = new ASMPushStack(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR));
		func.instructions.add(push);
		st.push(REGISTER.LR, REGISTER.FP);
		
		/* Save Stack from caller perspective */
		ASMMov fpMov = new ASMMov(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.SP));
		func.instructions.add(fpMov);
		
		/* Cast all statements and add all instructions */
		for (Statement s : f.statements) {
			func.instructions.addAll(AsNStatement.cast(s, r, st).getInstructions());
		}
		
		/* Check if other function is called within this function */
		boolean hasCall = func.instructions.stream().filter(x -> x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL).count() > 0;
		
		/* Branch back */
		func.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOperand(REGISTER.LR)));
		
		List<REGISTER> used = func.getUsed();
		
		/* Reset stack to state from caller */
		if (!hasCall && !func.paramsInStack()) {
			/* No other function was called and no parameters in the stack, saving lr not nessesary */
			if (used.isEmpty()) func.instructions.remove(push);
			else {
				/* Patch used registers into push instruction at the start */
				push.operands.clear();
				used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
				
				for (int i = 0; i < func.instructions.size(); i++) {
					if (func.instructions.get(i) instanceof ASMBranch) {
						ASMBranch branch = (ASMBranch) func.instructions.get(i);
						if (branch.type == BRANCH_TYPE.BX) {
							ASMPopStack pop = new ASMPopStack();
							used.stream().forEach(x -> pop.operands.add(new RegOperand(x)));
							
							func.instructions.add(i, pop);
							i++;
						}
					}
				}
				
				func.patchFramePointerAddressing(push.operands.size() * 4);
			}
			func.instructions.remove(fpMov);
		}
		else {
			/* Patch used registers into push instruction at the start */
			push.operands.clear();
			used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
			push.operands.add(new RegOperand(REGISTER.FP));
			push.operands.add(new RegOperand(REGISTER.LR));
			
			/* Inject register restoring and stack resetting before returns */
			for (int i = 0; i < func.instructions.size(); i++) {
				if (func.instructions.get(i) instanceof ASMBranch) {
					ASMBranch branch = (ASMBranch) func.instructions.get(i);
					if (branch.type == BRANCH_TYPE.BX) {
						func.instructions.add(i, new ASMMov(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.FP)));
						
						/* Build Instruction that pops all used regs, FP and LR */
						ASMPopStack pop = new ASMPopStack();
						used.stream().forEach(x -> pop.operands.add(new RegOperand(x)));
						pop.operands.add(new RegOperand(REGISTER.FP));
						pop.operands.add(new RegOperand(REGISTER.LR));
						
						/* Restore FP and LR */
						func.instructions.add(i + 1, pop);
						i += 2;
					}
				}
			}
			
			func.patchFramePointerAddressing(push.operands.size() * 4);
		}
		
		/* Patch Frame Pointer Stack Addressing */
		
		return func;
	}
	
	public void patchFramePointerAddressing(int offset) {
		for (ASMInstruction ins : this.instructions) {
			if (ins instanceof ASMMemOp) {
				ASMMemOp memOp = (ASMMemOp) ins;
				if (memOp.op0.reg == REGISTER.FP) {
					/* Frame Pointer relative addressing */
					memOp.op1 = new ImmOperand(((ImmOperand) memOp.op1).value + offset);
				}
			}
		}
	}
	
	/**
	 * Check if parameters are passed in the stack.
	 */
	public boolean paramsInStack() {
		return this.parameterMapping.stream().map(x -> x.tpl_2()).filter(x -> x == -1).count() > 0;
	}
	
	public List<REGISTER> getUsed() {
		REGISTER [] notIncluded = {REGISTER.R0, REGISTER.R1, REGISTER.R2, REGISTER.FP, REGISTER.SP, REGISTER.LR};
		List<REGISTER> used = new ArrayList();
		
		this.instructions.stream().forEach(x -> {
			if (x instanceof ASMMov) {
				ASMMov mov = (ASMMov) x;
				
				boolean use = true;
				for (REGISTER r : notIncluded) if (r == mov.target.reg) use = false;
				
				if (use) used.add(mov.target.reg);

			}
		});
		
		/* Filter duplicates */
		for (int i = 0; i < used.size(); i++) {
			for (int a = i + 1; a < used.size(); a++) {
				if (used.get(a) == used.get(i)) {
					used.remove(a);
					a--;
				}
			}
		}
		
		return used;
	}
	
	public List<Pair<Declaration, Integer>> getParameterMapping() {
		List<Pair<Declaration, Integer>> mapping = new ArrayList();
		
		int r = 0;
		for (Declaration dec : this.source.parameters) {
			int wordSize = dec.type.wordSize;
			if (wordSize == 1 && r < 3) {
				/* Load in register */
				mapping.add(new Pair(dec, r));
				r++;
			}
			else {
				/* Load in stack */
				mapping.add(new Pair(dec, -1));
			}
		}
		
		return mapping;
	}

}
