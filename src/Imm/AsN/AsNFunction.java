package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNStatement;

public class AsNFunction extends AsNNode {

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
		
		/* Function Header and Entry Label */
		func.instructions.add(new ASMSectionAnnotation(SECTION.GLOBAL, func.source.functionName));
		func.instructions.add(new ASMLabel(func.source.functionName));
		
		/* TODO */
		/* Save FP and lr */
		ASMPushStack push = new ASMPushStack(new RegOperand(REGISTER.R3), new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR));
		func.instructions.add(push);
		st.push(REGISTER.R3, REGISTER.FP, REGISTER.LR);
		
		/* Save Stack from caller perspective */
		ASMMov fpMov = new ASMMov(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.SP));
		func.instructions.add(fpMov);
		
		/* TODO */
		/* Set Params in Registers */
		for (int i = 0; i < f.parameters.size(); i++) {
			r.getReg(i).setDeclaration(f.parameters.get(i));
		}
		
		for (Statement s : f.statements) {
			func.instructions.addAll(AsNStatement.cast(s, r, st).getInstructions());
		}
		
		/* Check if other function is called within this function */
		boolean hasCall = func.instructions.stream().filter(x -> x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL).count() > 0;
		
		/* Branch back */
		func.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOperand(REGISTER.LR)));
		
		List<REGISTER> used = func.getUsed();
		
		/* Reset stack to state from caller */
		if (!hasCall) {
			/* No other function was called, saving lr not nessesary */
			if (used.isEmpty()) func.instructions.remove(push);
			else {
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
			}
			func.instructions.remove(fpMov);
		}
		else {
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
						
						/* Build Instruction with that pops all used regs, FP and LR */
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
		}
		
		/**
		 * TODO
		 * Go through all generated instructions, list all regs that need to be saved, 
		 * insert that push/pop and move fp, sp statement before every bx lr statement
		 */
		
		return func;
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

}
