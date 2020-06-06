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
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMMemOp;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp;
import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
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
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Util.Pair;

public class AsNFunction extends AsNCompoundStatement {

			/* --- FIELDS --- */
	public List<Pair<Declaration, Integer>> parameterMapping;
	
	public Function source;
	
	public ASMLabel copyLoopEscape;
	
	
			/* --- METHODS --- */
	/**
	 * Casts given syntax element based on the given reg set to a asm function node. 
	 * @throws CTX_EXCEPTION 
	 */
	public static AsNFunction cast(Function f, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION, CTX_EXCEPTION {
		AsNFunction func = new AsNFunction();
		f.castedNode = func;
		func.source = f;
		
		/* 
		 * Add default mappings for library operators. These can be called by assign arith, 
		 * and therefore maybe have no registered mapping.
		 */
		if (f.path.build().equals("__op_div") || f.path.build().equals("__op_mod")) {
			if (!f.manager.containsMapping(new ArrayList())) {
				f.manager.addProvisoMapping(new INT(), new ArrayList());
			}
		}
		
		LabelGen.reset();
		LabelGen.funcPrefix = f.path.build();
		
		if (f.signals) func.copyLoopEscape = new ASMLabel(LabelGen.getLabel());
		
		List<ASMInstruction> all = new ArrayList();
		
		for (Declaration d : f.parameters) {
			if (d.getType() instanceof FUNC) {
				FUNC f0 = (FUNC) d.getType();
				
				/* 
				 * Predicate function heads are not casted, set a casted node here to prevent confusion later.
				 * The isLambdaHead flag will indicate that this function is a predicate.
				 */
				AsNFunction casted = new AsNFunction();
				casted.source = f0.funcHead;
				
				/* May be null due to anonymous call */
				if (f0.funcHead != null) f0.funcHead.castedNode = casted;
			}
		}
		
		/*
		 * Change names of proviso mappings if the types are word-size equal.
		 * The translation will result in the same assembly, so we dont have to do it again.
		 * The name is set to the first occurrence of the similar mappings. Down below
		 * the name is checked if its already in a list of translated mappings.
		 */
		for (int i = 0; i < f.manager.provisosCalls.size(); i++) {
			Pair<String, Pair<TYPE, List<TYPE>>> call0 = f.manager.provisosCalls.get(i);
			for (int a = i + 1; a < f.manager.provisosCalls.size(); a++) {
				Pair<String, Pair<TYPE, List<TYPE>>> call1 = f.manager.provisosCalls.get(a);
				
				if (!call0.first.equals(call1.first)) {
					boolean equal = true;
					equal &= call0.second.first.wordsize() == call1.second.first.wordsize();
					for (int k = 0; k < call0.second.second.size(); k++) {
						equal &= call0.second.second.get(k).wordsize() == call1.second.second.get(k).wordsize();
					}
					
					/* Mappings are of equal types, let one point to the other */
					if (equal) {
						call1.first = call0.first;
					}
				}
			}
		}
		
		/* 
		 * List that contains all the names of the proviso calls that were already translated.
		 * This is used to check wether to translate the current proviso mapping again. The name
		 * would be the same since it was changed up below.
		 */
		List<String> translated = new ArrayList();
		
		for (int k = 0; k < f.manager.provisosCalls.size(); k++) {
			/* Reset regs and stack */
			r = new RegSet();
			st = new StackSet();
			
			/* Check if mapping was already translated, if yes, skip */
			if (translated.contains(f.manager.provisosCalls.get(k).first)) continue;
			else translated.add(f.manager.provisosCalls.get(k).first);
			
			/* Set the current proviso call scheme if its not the default scheme */
			if (!f.manager.provisosCalls.get(k).first.equals("")) {
				f.setContext(f.manager.provisosCalls.get(k).second.second);
			}
			
			/* Setup Parameter Mapping */
			func.parameterMapping = func.getParameterMapping();
			
			/* Apply parameters to RegSet and StackSet */
			for (Pair<Declaration, Integer> p : func.parameterMapping) {
				if (p.getSecond() == -1) 
					/* Paramter is in stack, push in stackSet */
					st.push(p.getFirst());
				else 
					/* Load Declaration into register */
					r.getReg(p.getSecond()).setDeclaration(p.getFirst());
			}
			
			/* Create the function head label */
			String funcLabel = func.source.path.build() + f.manager.provisosCalls.get(k).first;
			
			/* Function address getter for lambda */
			if (f.isLambdaTarget) {
				ASMLabel l = new ASMLabel("lambda_" + funcLabel, true);
				l.comment = new ASMComment("Function address getter for predication");
				func.instructions.add(l);
				
				func.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.PC), new ImmOperand(8)));
				
				/* Branch back via sys jump */
				func.instructions.add(new ASMMov(new RegOperand(REGISTER.PC), new RegOperand(REGISTER.R10)));
			}
			
			/* Function Header and Entry Label, add proviso specific postfix */
			ASMLabel label = new ASMLabel(funcLabel, true);
			
			/* Generate comment with function name and potential proviso types */
			String com = "";
			if (f.manager.provisosCalls.get(k).first.equals("")) {
				com = "Function: " + f.path.build();
			}
			else {
				com = ((k == 0)? "Function: " + f.path.build() + ", " : "") + ((f.manager.provisosTypes.isEmpty())? "" : "Provisos: ");
				
				/* Create a String that lists all proviso mappings that this version of the function represents */
				for (int z = k; z < f.manager.provisosCalls.size(); z++) {
					if (f.manager.provisosCalls.get(z).first.equals(f.manager.provisosCalls.get(k).first)) {
						List<TYPE> types = f.manager.provisosCalls.get(z).second.second;
						for (int x = 0; x < types.size(); x++) {
							com += types.get(x).typeString() + ", ";
						}
						com = com.trim().substring(0, com.trim().length() - 1);
						
						if (z < f.manager.provisosCalls.size() - 1) com += " | ";
					}
				}
			}
			label.comment = new ASMComment(com);
			
			func.instructions.add(label);
			
			/* Save FP and lr by default */
			ASMPushStack push = new ASMPushStack(new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR));
			push.optFlags.add(OPT_FLAG.FUNC_CLEAN);
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
			boolean hasCall = func.instructions.stream().filter(x -> {
				return x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL ||
					   x instanceof ASMAdd && ((ASMAdd) x).target.reg == REGISTER.LR;
			}).count() > 0;
			
			/* Jumplabel to centralized function return */
			ASMLabel funcReturn = new ASMLabel(LabelGen.getLabel());
			
			List<REGISTER> used = func.getUsed();
			
			
			if (!hasCall && !func.hasParamsInStack() && f.getReturnType().wordsize() == 1 && !f.signals) {
				if (used.isEmpty()) 
					func.instructions.remove(push);
				else {
					/* Patch used registers into push instruction at the start */
					push.operands.clear();
					used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
					
					func.patchBxToB(funcReturn);
				}
				
				if (!st.newDecsOnStack && f.getReturnType().wordsize() == 1) func.instructions.remove(fpMov);
			}
			else {
				/* Patch used registers into push instruction at the start */
				push.operands.clear();
				used.stream().forEach(x -> push.operands.add(new RegOperand(x)));
				push.operands.add(new RegOperand(REGISTER.FP));
				if (hasCall) push.operands.add(new RegOperand(REGISTER.LR));
				
				func.patchBxToB(funcReturn);
			}
			
			
			/* Patch offset based on amount of pushed registers excluding LR and FP */
			func.patchFramePointerAddressing(push.operands.size() * 4);
			
			/* If function signals exceptions, add escape target for exceptions to jump to */
			if (f.signals) {
				func.instructions.add(func.copyLoopEscape);
			}
			
			ASMPopStack pop = null;
			
			if (hasCall || func.hasParamsInStack() || !used.isEmpty() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || f.signals) {
				/* Add centralized stack reset and register restoring */
				func.instructions.add(funcReturn);
				
				/* Check if exception was thrown */
				if (f.signals) func.instructions.add(new ASMCmp(new RegOperand(REGISTER.R12), new ImmOperand(0)));
				
				pop = new ASMPopStack();
				for (REGISTER reg : used) pop.operands.add(new RegOperand(reg));
				
				if (hasCall || func.hasParamsInStack() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || f.signals) {
					/* Backup SP in R2 */
					if (f.getReturnType().wordsize() > 1 || f.signals) {
						func.instructions.add(new ASMMov(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.SP)));
					}
					
					func.instructions.add(new ASMMov(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.FP)));
				
					if (hasCall || func.hasParamsInStack() || f.getReturnType().wordsize() > 1 || f.signals) {
						/* Need to restore registers */
						pop.operands.add(new RegOperand(REGISTER.FP));
						if (hasCall) pop.operands.add(new RegOperand(REGISTER.LR));
					}
				}
				
				pop.optFlags.add(OPT_FLAG.FUNC_CLEAN);
				func.instructions.add(pop);
			}
			
			int size = 0;
			for (Pair<Declaration, Integer> p  : func.getParameterMapping()) {
				if (p.getSecond() == -1) {
					/* Stack shrinks by parameter word size */
					size += p.getFirst().getType().wordsize();
				}
			}
			
			if (size != 0) {
				func.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(size * 4)));
			}
			
			ASMLabel singleWordSkip = new ASMLabel(LabelGen.getLabel());
			if (f.getReturnType().wordsize() == 1 && f.signals) {
				func.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(singleWordSkip)));
			}
			
			if (f.getReturnType().wordsize() > 1 || f.signals) {
				if (f.signals && f.getReturnType().wordsize() > 1) {
					/* 
					 * No exception, move word size of return type in R0, if execption 
					 * were thrown, the word size would already be in R0 
					 */
					ASMMov mov = new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(f.getReturnType().wordsize() * 4), new Cond(COND.EQ));
					mov.optFlags.add(OPT_FLAG.WRITEBACK);
					func.instructions.add(mov);
				}
				else if (f.getReturnType().wordsize() > 1) {
					/* Function does not signal, move word size of return type in R0 */
					ASMMov mov = new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(f.getReturnType().wordsize() * 4));
					mov.optFlags.add(OPT_FLAG.WRITEBACK);
					func.instructions.add(mov);
				}
				
				/* End address of return in stack */
				func.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R0)));
				
				/* Copy the data from the top of the stack to the return stack location */
				AsNBody.branchToCopyRoutine(func);
			}
			
			if (f.getReturnType().wordsize() == 1 && f.signals) {
				func.instructions.add(singleWordSkip);
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
				/* Only patch bx and instructions that are not part of exceptional exit */
				if (branch.type == BRANCH_TYPE.BX && !branch.optFlags.contains(OPT_FLAG.EXC_EXIT)) {
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
		REGISTER [] notIncluded = {REGISTER.R0, REGISTER.R1, REGISTER.R2, REGISTER.R12, REGISTER.FP, REGISTER.SP, REGISTER.LR, REGISTER.PC};
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
