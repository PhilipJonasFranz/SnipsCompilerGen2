package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.CTX_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMMemOp;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp;
import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Statement.AsNCompoundStatement;
import Imm.TYPE.TYPE;
import Util.Pair;

public class AsNFunction extends AsNCompoundStatement {

			/* --- FIELDS --- */
	public List<Pair<Declaration, Integer>> parameterMapping;
	
	public Function source;
	
	
			/* --- METHODS --- */
	/**
	 * Casts given syntax element based on the given reg set to a asm function node. 
	 */
	public static AsNFunction cast(Function f, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNFunction func = new AsNFunction();
		f.castedNode = func;
		func.source = f;
		
		List<ASMInstruction> all = new ArrayList();
		
		for (int k = 0; k < f.manager.provisosCalls.size(); k++) {
			/* Reset regs and stack */
			r = new RegSet();
			st = new StackSet();
			
			if (!f.manager.provisosCalls.get(k).first.equals("")) {
				/* Set the current proviso call scheme */
				try {
					f.setContext(f.manager.provisosCalls.get(k).second.second);
				} catch (CTX_EXCEPTION e) {
					
				}
			}
			
			// TODO Do for specific proviso cases
			/* Setup Parameter Mapping */
			func.parameterMapping = func.getParameterMapping();
			
			/* Set Params in Registers */
			for (Pair<Declaration, Integer> p : func.parameterMapping) {
				if (p.getSecond() == -1) 
					/* Paramter is in stack, push in stackSet */
					st.push(p.getFirst());
				else 
					/* Load Declaration into register */
					r.getReg(p.getSecond()).setDeclaration(p.getFirst());
			}
			
			/* Function Header and Entry Label, add proviso specific postfix */
			ASMLabel label = new ASMLabel(func.source.functionName + f.manager.provisosCalls.get(k).first, true);
			
			String com = "";
			if (f.manager.provisosCalls.get(k).first.equals("")) {
				com = "Function: " + f.functionName;
			}
			else {
				com = ((k == 0)? "Function: " + f.functionName + ", " : "") + "Provisos: ";
				List<TYPE> types = f.manager.provisosCalls.get(k).second.second;
				for (int x = 0; x < types.size(); x++) {
					com += types.get(x).typeString() + ", ";
				}
				com = com.trim().substring(0, com.trim().length() - 1);
			}
			
			label.comment = new ASMComment(com);
			
			func.instructions.add(label);
			
			/* Save FP and lr by default */
			ASMPushStack push = new ASMPushStack(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR));
			func.instructions.add(push);
			st.push(REGISTER.LR, REGISTER.FP);
			
			/* Save Stackpointer from caller perspective */
			ASMMov fpMov = new ASMMov(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.SP));
			func.instructions.add(fpMov);
			
			/* Save parameters in register */
			func.clearReg(r, st, 0, 1, 2);
			
			for (int i = 0; i < f.parameters.size(); i++) {
				Declaration dec = f.parameters.get(i);
				if (r.declarationLoaded(dec)) {
					if (func.hasAddressReference(f, dec)) {
						int location = r.declarationRegLocation(dec);
						
						ASMPushStack push0 = new ASMPushStack(new RegOperand(location));
						push0.comment = new ASMComment("Push declaration on stack, referenced by addressof.");
						func.instructions.add(push0);
						
						st.push(dec);
						r.free(location);
					}
				}
			}
			
			/* Cast all statements and add all instructions */
			for (int i = 0; i < f.body.size(); i++) { 
				func.loadStatement(f, f.body.get(i), r, map, st);
				//func.instructions.addAll(AsNStatement.cast(s, r, map, st).getInstructions());
			}
			
			
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
			
			if (f.manager.provisosCalls.size() > 1 && k < f.manager.provisosCalls.size() - 1) {
				func.instructions.add(new ASMSeperator());
			}
			
			if (!f.manager.provisosTypes.isEmpty()) {
				all.addAll(func.instructions);
				func.instructions.clear();
			}
		}
		
		if (!f.manager.provisosTypes.isEmpty()) func.instructions.addAll(all);
	
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
			if (ins instanceof ASMStackOp) {
				ASMStackOp stackOp = (ASMStackOp) ins;
				if (stackOp.op0 != null && stackOp.op0.reg == REGISTER.FP) {
					if (stackOp.op1 instanceof PatchableImmOperand) {
						PatchableImmOperand op = (PatchableImmOperand) stackOp.op1;
						
						/* Patch the offset for parameters because they are located under the pushed regs,
						 * dont patch local data since its located above the pushed regs.
						 */
						if (op.dir == PATCH_DIR.UP) {
							int val = op.patch(offset);
							stackOp.op1 = new ImmOperand(val);
						}
					}
					else throw new CGEN_EXCEPTION(this.source.getSource(), "Cannot patch non-patchable imm operand!");
				}
			}
			else if (ins instanceof ASMBinaryData) {
				ASMBinaryData binary = (ASMBinaryData) ins;
				
				if (binary.op0 != null && binary.op0.reg == REGISTER.FP) {
					if (binary.op1 instanceof PatchableImmOperand) {
						PatchableImmOperand op = (PatchableImmOperand) binary.op1;
						
						if (op.dir == PATCH_DIR.UP) {
							int val = op.patch(offset);
							binary.op1 = new ImmOperand(val);
						}
					}
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp mem = (ASMMemOp) ins;
				
				if (mem.op0 != null && mem.op0 instanceof RegOperand && ((RegOperand) mem.op0).reg == REGISTER.FP) {
					if (mem.op1 instanceof PatchableImmOperand) {
						PatchableImmOperand op = (PatchableImmOperand) mem.op1;
						
						if (op.dir == PATCH_DIR.UP) {
							int val = op.patch(offset);
							mem.op1 = new ImmOperand(val);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Check if parameters are passed in the stack.
	 */
	public boolean hasParamsInStack() {
		return this.parameterMapping.stream().map(x -> x.getSecond()).filter(x -> x == -1).count() > 0;
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
	public List<Pair<Declaration, Integer>> getParameterMapping() {
		int r = 0;
		List<Pair<Declaration, Integer>> mapping = new ArrayList();
		
		for (Declaration dec : this.source.parameters) {
			//dec.print(0, true);
			int wordSize = dec.getType().wordsize();
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
