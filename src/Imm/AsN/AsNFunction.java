package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Opt.ASMOptimizer;
import Exc.CGEN_EXC;
import Exc.CTX_EXC;
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
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Function.ProvisoMapping;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNCompoundStatement;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Res.Const;
import Util.Pair;

public class AsNFunction extends AsNCompoundStatement {

			/* --- FIELDS --- */
	public List<Pair<Declaration, Integer>> parameterMapping;
	
	public Function source;
	
	public ASMLabel copyLoopEscape;
	
	
			/* --- METHODS --- */
	/**
	 * Casts given syntax element based on the given reg set to a asm function node. 
	 * @throws CTX_EXC 
	 */
	public static AsNFunction cast(Function f, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC, CTX_EXC {
		AsNFunction func = new AsNFunction();
		f.castedNode = func;
		func.source = f;
		
		/* 
		 * Add default mappings for library operators. These can be called by assign arith, 
		 * and therefore maybe have no registered mapping.
		 */
		if (f.path.build().equals("__op_div") || f.path.build().equals("__op_mod")) {
			if (!f.containsMapping(new ArrayList())) {
				f.addProvisoMapping(new INT(), new ArrayList());
			}
		}
		
		LabelGen.reset();
		LabelGen.funcPrefix = f.path.build();
		
		if (f.signals()) func.copyLoopEscape = new ASMLabel(LabelGen.getLabel());
		
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
		for (int i = 0; i < f.provisosCalls.size(); i++) {
			ProvisoMapping call0 = f.provisosCalls.get(i);
			for (int a = i + 1; a < f.provisosCalls.size(); a++) {
				ProvisoMapping call1 = f.provisosCalls.get(a);
				
				if (!call0.provisoPostfix.equals(call1.provisoPostfix)) {
					boolean equal = true;
					
					/* Check for equal return type */
					equal &= call0.returnType.wordsize() == call1.returnType.wordsize();
					
					/* Check for equal parameter types */
					for (int k = 0; k < call0.provisoMapping.size(); k++) 
						equal &= call0.provisoMapping.get(k).wordsize() == call1.provisoMapping.get(k).wordsize();
					
					/* 
					 * Mappings are of equal types, set label gen postfix of 
					 * this one to the other equal one 
					 */
					if (equal) 
						call1.provisoPostfix = call0.provisoPostfix;
				}
			}
		}
		
		/* 
		 * List that contains all the names of the proviso calls that were already translated.
		 * This is used to check wether to translate the current proviso mapping again. The name
		 * would be the same since it was changed up below.
		 */
		List<String> translated = new ArrayList();
		
		for (int k = 0; k < f.provisosCalls.size(); k++) {
			/* Reset regs and stack */
			r = new RegSet();
			st = new StackSet();
			
			/* Check if mapping was already translated, if yes, skip */
			if (translated.contains(f.provisosCalls.get(k).provisoPostfix)) continue;
			else translated.add(f.provisosCalls.get(k).provisoPostfix);
			
			/* Set the current proviso call scheme if its not the default scheme */
			if (!f.provisosCalls.get(k).provisoPostfix.equals("")) 
				f.setContext(f.provisosCalls.get(k).provisoMapping);
			
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
			String funcLabel = func.source.path.build() + f.provisosCalls.get(k).provisoPostfix;
			
			/* Function address getter for lambda */
			if (f.isLambdaTarget) {
				ASMLabel l = new ASMLabel("lambda_" + funcLabel, true);
				l.comment = new ASMComment("Function address getter for predication");
				func.instructions.add(l);
				
				func.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.PC), new ImmOp(8)));
				
				/* Branch back via sys jump */
				func.instructions.add(new ASMMov(new RegOp(REG.PC), new RegOp(REG.R10)));
			}
			
			/* Function Header and Entry Label, add proviso specific postfix */
			ASMLabel label = new ASMLabel(funcLabel, true);
			
			/* Generate comment with function name and potential proviso types */
			String com = "";
			if (f.provisosCalls.get(k).provisoPostfix.equals("")) {
				com = "Function: " + f.path.build();
			}
			else {
				com = ((k == 0)? "Function: " + f.path.build() + ", " : "") + ((f.provisosTypes.isEmpty())? "" : "Provisos: ");
				
				/* Create a String that lists all proviso mappings that this version of the function represents */
				for (int z = k; z < f.provisosCalls.size(); z++) {
					if (f.provisosCalls.get(z).provisoPostfix.equals(f.provisosCalls.get(k).provisoPostfix)) {
						List<TYPE> types = f.provisosCalls.get(z).provisoMapping;
						
						for (int x = 0; x < types.size(); x++) 
							com += types.get(x).provisoFree().typeString() + ", ";
						
						com = com.trim().substring(0, com.trim().length() - 1);
						
						if (z < f.provisosCalls.size() - 1) com += " | ";
					}
				}
			}
			label.comment = new ASMComment(com);
			
			func.instructions.add(label);
			
			/* Save FP and lr by default */
			ASMPushStack push = new ASMPushStack(new RegOp(REG.FP), new RegOp(REG.LR));
			push.optFlags.add(OPT_FLAG.FUNC_CLEAN);
			func.instructions.add(push);
			st.push(REG.LR, REG.FP);
			
			/* Save Stackpointer from caller perspective */
			ASMMov fpMov = new ASMMov(new RegOp(REG.FP), new RegOp(REG.SP));
			ASMMov fpMovBack = null;
			func.instructions.add(fpMov);
			
			int paramsWithRef = 0;
			
			/* Save parameters in register or stack if the have a address reference */
			for (int i = 0; i < 3; i++) {
				if (r.getReg(i).declaration != null) {
					Declaration d = r.getReg(i).declaration;
					
					boolean hasRef = false;
					for (Statement s : f.body) 
						hasRef |= AsNCompoundStatement.hasAddressReference(s, d);
					
					if (hasRef) {
						ASMPushStack init = new ASMPushStack(new RegOp(i));
						init.optFlags.add(OPT_FLAG.STRUCT_INIT);
						init.comment = new ASMComment("Push declaration on stack, referenced by addressof.");
						
						/* Only do it if variable was used */
						if (d.last == null || !d.last.equals(d)) {
							func.instructions.add(init);
							paramsWithRef++;
						}
						
						st.push(d);
						r.free(i);
					}
					else func.clearReg(r, st, i);
				}
			}
			
			/* Cast all statements and add all instructions */
			for (int i = 0; i < f.body.size(); i++) 
				func.loadStatement(f, f.body.get(i), r, map, st);
			
			/* Check if any instruction reads or writes to the frame-pointer */
			boolean fpInteraction = false;
			for (ASMInstruction x : func.instructions) {
				if (x.equals(fpMov) || x.equals(push)) continue;
				fpInteraction |= ASMOptimizer.overwritesReg(x, REG.FP) || ASMOptimizer.readsReg(x, REG.FP);
			}
				
			/* Check if other function is called within this function */
			boolean hasCall = func.instructions.stream().filter(x -> {
				return x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL ||
					   x instanceof ASMAdd && ((ASMAdd) x).target.reg == REG.LR;
			}).count() > 0;
			
			/* Jumplabel to centralized function return */
			ASMLabel funcReturn = new ASMLabel(LabelGen.getLabel());
			
			List<REG> used = func.getUsed();
			
			
			if (paramsWithRef == 0 && !func.hasParamsInStack() && !f.signals() && !st.newDecsOnStack && f.getReturnType().wordsize() == 1 && !fpInteraction) {
				func.instructions.remove(fpMov);
				push.operands.remove(0);
			}
			
			if (!hasCall && !func.hasParamsInStack() && f.getReturnType().wordsize() == 1 && !f.signals() && paramsWithRef == 0) {
				if (used.isEmpty()) 
					func.instructions.remove(push);
				else {
					/* Patch used registers into push instruction at the start */
					push.operands.clear();
					used.stream().forEach(x -> push.operands.add(new RegOp(x)));
					
					func.patchBxToB(funcReturn);
				}		
			}
			else {
				/* Patch used registers into push instruction at the start */
				push.operands.clear();
				used.stream().forEach(x -> push.operands.add(new RegOp(x)));
				
				if (func.hasParamsInStack() || f.signals() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || fpInteraction) push.operands.add(new RegOp(REG.FP));
				
				if (hasCall) push.operands.add(new RegOp(REG.LR));
				
				func.patchBxToB(funcReturn);
			}
			
			/* 
			 * Main specific removals, need to remove from push here already since
			 * FP-relativ addressing is patched down below and is dependent on pushed
			 * regs.
			 */
			if (f.path.build().equals("main")) {
				for (int i = 0; i < push.operands.size(); i++) {
					if (push.operands.get(i).reg != REG.FP && push.operands.get(i).reg != REG.LR) {
						push.operands.remove(i);
						i--;
					}
				}
			}
			
			/* Patch offset based on amount of pushed registers excluding LR and FP */
			func.patchFramePointerAddressing(push.operands.size() * 4);
			
			/* If function signals() exceptions, add escape target for exceptions to jump to */
			if (f.signals()) func.instructions.add(func.copyLoopEscape);
			
			ASMPopStack pop = null;
			
			if (paramsWithRef > 0 || hasCall || func.hasParamsInStack() || !used.isEmpty() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || f.signals()) {
				/* Add centralized stack reset and register restoring */
				func.instructions.add(funcReturn);
				
				/* Check if exception was thrown */
				if (f.signals()) func.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(0)));
				
				pop = new ASMPopStack();
				for (REG reg : used) pop.operands.add(new RegOp(reg));
				
				if (paramsWithRef > 0 || hasCall || func.hasParamsInStack() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || f.signals()) {
					/* Backup SP in R2 */
					if (f.getReturnType().wordsize() > 1 || f.signals()) 
						func.instructions.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.SP)));
					
					if (paramsWithRef > 0 || func.hasParamsInStack() || f.signals() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || fpInteraction) {
						fpMovBack = new ASMMov(new RegOp(REG.SP), new RegOp(REG.FP));
						func.instructions.add(fpMovBack);
					}
				
					if (paramsWithRef > 0 || hasCall || func.hasParamsInStack() || f.getReturnType().wordsize() > 1 || f.signals()) {
						/* Need to restore registers */
						if (paramsWithRef > 0 || func.hasParamsInStack() || f.signals() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || fpInteraction) pop.operands.add(new RegOp(REG.FP));
						
						/* Function has a call, must save LR */
						if (hasCall) pop.operands.add(new RegOp(REG.LR));
					}
				}
				
				/* Set relation for optimizer */
				push.popCounterpart = pop;
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
			
			if (size != 0 && !f.path.build().equals("main")) {
				func.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(size * 4)));
			}
			
			ASMLabel singleWordSkip = new ASMLabel(LabelGen.getLabel());
			if (f.getReturnType().wordsize() == 1 && f.signals()) {
				func.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(singleWordSkip)));
			}
			
			if (f.getReturnType().wordsize() > 1 || f.signals()) {
				if (f.signals() && f.getReturnType().wordsize() > 1) {
					/* 
					 * No exception, move word size of return type in R0, if execption 
					 * were thrown, the word size would already be in R0 
					 */
					ASMMov mov = new ASMMov(new RegOp(REG.R0), new ImmOp(f.getReturnType().wordsize() * 4), new Cond(COND.EQ));
					mov.optFlags.add(OPT_FLAG.WRITEBACK);
					func.instructions.add(mov);
				}
				else if (f.getReturnType().wordsize() > 1) {
					/* Function does not signal, move word size of return type in R0 */
					ASMMov mov = new ASMMov(new RegOp(REG.R0), new ImmOp(f.getReturnType().wordsize() * 4));
					mov.optFlags.add(OPT_FLAG.WRITEBACK);
					func.instructions.add(mov);
				}
				
				/* End address of return in stack */
				func.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R2), new RegOp(REG.R0)));
				
				/* Copy the data from the top of the stack to the return stack location */
				AsNBody.branchToCopyRoutine(func);
			}
			
			if (f.getReturnType().wordsize() == 1 && f.signals()) {
				func.instructions.add(singleWordSkip);
			}
			
			/* Branch back */
			func.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOp(REG.LR)));

			/* Main specific removals */
			if (f.path.build().equals("main") && pop != null) {
				for (int i = 0; i < pop.operands.size(); i++) {
					if (pop.operands.get(i).reg != REG.FP && pop.operands.get(i).reg != REG.LR) {
						pop.operands.remove(i);
						i--;
					}
				}
			}
			
			if (push.operands.isEmpty()) {
				func.instructions.remove(push);
				func.instructions.remove(pop);
			}
			
			if (f.provisosCalls.size() > 1 && k < f.provisosCalls.size() - 1) {
				func.instructions.add(new ASMSeperator());
			}
			
			if (!f.provisosTypes.isEmpty()) {
				all.addAll(func.instructions);
				func.instructions.clear();
			}
		}
		
		if (!f.provisosTypes.isEmpty()) func.instructions.addAll(all);
	
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
					branch.target = new LabelOp(funcReturn);
				}
			}
		}
	}
	
	public void patchFramePointerAddressing(int offset) throws CGEN_EXC {
		for (ASMInstruction ins : this.instructions) {
			if (ins instanceof ASMStackOp) {
				ASMStackOp stackOp = (ASMStackOp) ins;
				if (stackOp.op0 != null && stackOp.op0.reg == REG.FP) {
					if (stackOp.op1 instanceof PatchableImmOp) {
						PatchableImmOp op = (PatchableImmOp) stackOp.op1;
						
						/* Patch the offset for parameters because they are located under the pushed regs,
						 * dont patch local data since its located above the pushed regs.
						 */
						if (op.dir == PATCH_DIR.UP) {
							op.patch(offset);
							stackOp.op1 = new ImmOp(op.patchedValue, op);
						}
					}
					else throw new CGEN_EXC(this.source.getSource(), Const.CANNOT_PATCH_NON_PATCHABLE_IMM_OP);
				}
			}
			else if (ins instanceof ASMBinaryData) {
				ASMBinaryData binary = (ASMBinaryData) ins;
				
				if (binary.op0 != null && binary.op0.reg == REG.FP) {
					if (binary.op1 instanceof PatchableImmOp) {
						PatchableImmOp op = (PatchableImmOp) binary.op1;
						
						if (op.dir == PATCH_DIR.UP) {
							op.patch(offset);
							binary.op1 = new ImmOp(op.patchedValue, op);
						}
					}
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp mem = (ASMMemOp) ins;
				
				if (mem.op0 != null && mem.op0 instanceof RegOp && ((RegOp) mem.op0).reg == REG.FP) {
					if (mem.op1 instanceof PatchableImmOp) {
						PatchableImmOp op = (PatchableImmOp) mem.op1;
						
						if (op.dir == PATCH_DIR.UP) {
							op.patch(offset);
							mem.op1 = new ImmOp(op.patchedValue, op);
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
	public List<REG> getUsed() {
		REG [] notIncluded = {REG.R0, REG.R1, REG.R2, REG.R10, REG.R12, REG.FP, REG.SP, REG.LR, REG.PC};
		List<REG> used = new ArrayList();
		
		this.instructions.stream().forEach(x -> {
			if (x instanceof ASMMov) {
				ASMMov mov = (ASMMov) x;
				
				boolean use = true;
				for (REG r : notIncluded) if (r == mov.target.reg) use = false;
				
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
			if (dec.getType().wordsize() == 1 && r < 3 && !(dec.getType() instanceof STRUCT)) {
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
