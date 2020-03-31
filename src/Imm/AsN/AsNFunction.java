package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Stack.ASMMemOp;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNStatement;
import Util.Pair;

public class AsNFunction extends AsNNode {

			/* --- FIELDS --- */
	public List<Pair<Declaration, Integer>> parameterMapping;
	
	public Function source;
	
	
			/* --- METHODS --- */
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
			if (p.tpl_2() == -1) 
				/* Paramter is in stack, push in stackSet */
				st.push(p.t0);
			else 
				/* Load Declaration into register */
				r.getReg(p.tpl_2()).setDeclaration(p.tpl_1());
		}
		
		
		/* Function Header and Entry Label */
		func.instructions.add(new ASMLabel(func.source.functionName, true));
		
		/* Save FP and lr by default */
		ASMPushStack push = new ASMPushStack(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR));
		func.instructions.add(push);
		st.push(REGISTER.LR, REGISTER.FP);
		
		/* Save Stackpointer from caller perspective */
		ASMMov fpMov = new ASMMov(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.SP));
		func.instructions.add(fpMov);
		
		/* Save parameters in register */
		func.clearReg(r, st, 0, 1, 2);
		
		/* Cast all statements and add all instructions */
		for (Statement s : f.statements) 
			func.instructions.addAll(AsNStatement.cast(s, r, st).getInstructions());
		
		
		/* Check if other function is called within this function */
		boolean hasCall = func.instructions.stream().filter(x -> x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL).count() > 0;
		
		/* Jumplabel to centralized function return */
		ASMLabel funcReturn = new ASMLabel(LabelGen.getLabel());
		
		List<REGISTER> used = func.getUsed();
		
		
		if (!hasCall && !func.hasParamsInStack()) {
			if (used.isEmpty()) 
				func.instructions.remove(push);
			else {
				/* Patch used registers into push instruction at the start */
				push.operands.clear();
				used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
				
				func.patchBxToB(funcReturn);
			}
			
			if (!st.newDecsOnStack) func.instructions.remove(fpMov);
		}
		else {
			/* Patch used registers into push instruction at the start */
			push.operands.clear();
			used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
			push.operands.add(new RegOperand(REGISTER.FP));
			push.operands.add(new RegOperand(REGISTER.LR));
			
			func.patchBxToB(funcReturn);
		}
		
		
		/* Patch offset based on amount of pushed registers excluding LR and FP */
		if (!used.isEmpty()) 
			func.patchFramePointerAddressing(push.operands.size() * 4);
		
		
		if (hasCall || func.hasParamsInStack() || !used.isEmpty() || st.newDecsOnStack) {
			/* Add centralized stack reset and register restoring */
			func.instructions.add(funcReturn);
			
			ASMPopStack pop = new ASMPopStack();
			used.stream().forEach(x -> pop.operands.add(new RegOperand(x)));
			
			if (hasCall || func.hasParamsInStack() || st.newDecsOnStack) {
				func.instructions.add(new ASMMov(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.FP)));
			
				if (hasCall || func.hasParamsInStack()) {
					/* Need to restore registers */
					pop.operands.add(new RegOperand(REGISTER.FP));
					pop.operands.add(new RegOperand(REGISTER.LR));
				}
			}
			
			func.instructions.add(pop);
		}
		
		
		/* Branch back */
		func.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOperand(REGISTER.LR)));
	
		return func;
	}
	
	/**
	 * Replace bx lr with branch to function end for centralized stack reset and bx
	 */
	public void patchBxToB(ASMLabel funcReturn) {
		for (int i = 0; i < this.instructions.size(); i++) {
			if (this.instructions.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) this.instructions.get(i);
				if (branch.type == BRANCH_TYPE.BX) {
					branch.type = BRANCH_TYPE.B;
					branch.target = new LabelOperand(funcReturn);
				}
			}
		}
	}
	
	public void patchFramePointerAddressing(int offset) throws CGEN_EXCEPTION {
		for (ASMInstruction ins : this.instructions) {
			if (ins instanceof ASMMemOp) {
				ASMMemOp memOp = (ASMMemOp) ins;
				if (memOp.op0.reg == REGISTER.FP) {
					if (memOp.op1 instanceof PatchableImmOperand) {
						PatchableImmOperand op = (PatchableImmOperand) memOp.op1;
						
						/* Patch the offset for parameters because they are located under the pushed regs,
						 * dont patch local data since its located above the pushed regs.
						 */
						if (op.dir == PATCH_DIR.UP) {
							int val = op.patch(offset);
							memOp.op1 = new ImmOperand(val);
						}
					}
					else throw new CGEN_EXCEPTION(this.source.getSource(), "Cannot patch non-patchable imm operand!");
				}
			}
		}
	}
	
	/**
	 * Check if parameters are passed in the stack.
	 */
	public boolean hasParamsInStack() {
		return this.parameterMapping.stream().map(x -> x.tpl_2()).filter(x -> x == -1).count() > 0;
	}
	
	/**
	 * Return a list of all registers that were used. Registers 0-2 and FP, SP, LR, PC are excluded.
	 * @return
	 */
	public List<REGISTER> getUsed() {
		REGISTER [] notIncluded = {REGISTER.R0, REGISTER.R1, REGISTER.R2, REGISTER.FP, REGISTER.SP, REGISTER.LR, REGISTER.PC};
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
	
	/**
	 * Create a parameter mapping that assigns each parameter either a register or the stack
	 * as a location.
	 */
	private List<Pair<Declaration, Integer>> getParameterMapping() {
		int r = 0;
		List<Pair<Declaration, Integer>> mapping = new ArrayList();
		
		for (Declaration dec : this.source.parameters) {
			int wordSize = dec.type.wordSize;
			if (wordSize == 1 && r < 3) {
				/* Load in register */
				mapping.add(new Pair(dec, r));
				r++;
			}
			else 
				/* Load in stack */
				mapping.add(new Pair(dec, -1));
		}
		return mapping;
	}

}
