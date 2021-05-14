package Imm.AsN;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Directive.ASMDirective;
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
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.*;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Memory.Stack.ASMVPopStack;
import Imm.ASM.VFP.Memory.Stack.ASMVPushStack;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNCompoundStatement;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.TYPE;
import Opt.ASM.ASMOptimizer;
import Opt.AST.Util.Matcher;
import Res.Const;
import Res.Manager.RessourceManager;
import Snips.CompilerDriver;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import Util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AsNFunction extends AsNCompoundStatement {

			/* ---< FIELDS >--- */
	public List<Pair<Declaration, Integer>> parameterMapping;
	
	public Function source;
	
	public ASMLabel copyLoopEscape;

	public List<String> generatedLabels = new ArrayList();
	
	/* 
	 * List that contains all the names of the proviso calls that were already translated.
	 * This is used to check wether to translate the current proviso mapping again. The name
	 * would be the same since it was changed up below.
	 */
	public List<String> translated = new ArrayList();
	

			/* ---< METHODS >--- */
	/**
	 * Casts given syntax element based on the given reg set to a asm function node.
	 */
	public static AsNFunction cast(Function f, MemoryMap map) throws CGEN_EXC, CTEX_EXC {
		AsNFunction func = new AsNFunction();
		func.pushOnCreatorStack(f);
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
		
		LabelUtil.reset();
		LabelUtil.funcPrefix = f.path.build();
		LabelUtil.funcUID = (f.requireUIDInLabel)? f.UID : -1;
		
		if (f.signals()) func.copyLoopEscape = new ASMLabel(LabelUtil.getLabel());
		
		List<ASMInstruction> all = new ArrayList();
		
		for (Declaration d : f.parameters) {
			if (d.getType() instanceof FUNC f0) {
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
		
		for (int k = 0; k < f.provisosCalls.size(); k++) {
			
			if (f.inheritLink == null) {
				/* 
				 * Body is null, insert include directive instead, or build 
				 * object file only and this function is not from the main file
				 */
				if (!CompilerDriver.buildModulesRecurse && 
						(f.body == null || 
						!CompilerDriver.inputFile.getAbsolutePath().endsWith(f.getSource().sourceFile))) {
					
					/* Replace .hn with .sn in module link */
					String source = RessourceManager.instance.toASMPath(f.getSource().sourceFile);
					
					func.instructions.add(new ASMDirective(".include " + source + "@" + f.path + f.provisosCalls.get(k).getProvisoPostfix()));
					
					/* Check if required module exists */
					String mappedPath = RessourceManager.instance.resolve(source);
					if (RessourceManager.instance.getFile(mappedPath) == null) {
						AsNBody.progress.abort();
						new Message("Module '" + f.path + f.provisosCalls.get(k).getProvisoPostfix() + "' in '" + source + "' does not exist", Type.WARN);
						new Message("To create the missing module, use -R to recompile modules recursiveley", Type.WARN);
					}
					
					continue;
				}
				
				if (f.body == null) 
					throw new CGEN_EXC("Attempted to cast function without body: " + f.path.build());
			}
			
			/*
			 * Function was inherited into struct but not overridden there.
			 * We can relay to the cast of the parent implementation here.
			 * But, we still need to create the symbolic function so the linker/assembler
			 * recognizes it.
			 */
			if (f.inheritLink != null && f.body == null) {
				/* Create the function head label */
				String funcLabel = f.buildCallLabel(f.provisosCalls.get(k).provisoMapping);
				
				func.generatedLabels.add(funcLabel);
				
				/* Add .global label */
				ASMDirective globalFunction = new ASMDirective(".global " + funcLabel);
				func.instructions.add(globalFunction);
				
				
				/* Function Header and Entry Label, add proviso specific postfix */
				ASMLabel label = new ASMLabel(funcLabel, true);
				f.headLabelMap.put(LabelUtil.getProvisoPostfix(f.provisosCalls.get(k).provisoMapping), label);
				func.instructions.add(label);
				
				
				String call = f.buildInheritedCallLabel(f.provisosCalls.get(k).provisoMapping);
				
				ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(new ASMLabel(call)));
				branch.optFlags.add(OPT_FLAG.SYS_JMP);
				branch.comment = new ASMComment("Relay to inherited " + call);
				
				func.instructions.add(branch);
				continue;
			}
			
			LabelUtil.currentContext = f.provisosCalls.get(k).getProvisoPostfix();
			
			/* Reset regs and stack */
			RegSet r = new RegSet();
			StackSet st = new StackSet();
			
			/* Check if mapping was already translated, if yes, skip */
			if (func.translated.contains(f.provisosCalls.get(k).getProvisoPostfix())) continue;
			else func.translated.add(f.provisosCalls.get(k).getProvisoPostfix());
			
			/* Set the current proviso call scheme if its not the default scheme */
			if (!f.provisosCalls.get(k).getProvisoPostfix().equals("")) 
				f.setContext(f.provisosCalls.get(k).provisoMapping);
			
			/* Setup Parameter Mapping */
			func.parameterMapping = func.getParameterMapping();
			
			/* Apply parameters to RegSet and StackSet */
			for (Pair<Declaration, Integer> p : func.parameterMapping) {
				if (p.getSecond() == -1) 
					/* Paramter is in stack, push in stackSet */
					st.push(p.getFirst());
				else {
					if (p.getSecond() < 16) {
						/* Load Declaration into Rx register */
						r.getReg(p.getSecond()).setDeclaration(p.getFirst());
					}
					else {
						/* Load Declaration into Sx register */
						r.getVRegSet().getReg(p.getSecond() - 16).setDeclaration(p.getFirst());
					}
				}
			}
			
			/* Create the function head label */
			String funcLabel = f.buildCallLabel(f.provisosCalls.get(k).provisoMapping);
			
			func.generatedLabels.add(funcLabel);
			
			/* Add .global label */
			ASMDirective globalFunction = new ASMDirective(".global " + funcLabel);
			func.instructions.add(globalFunction);
			
			
			/* Function Header and Entry Label, add proviso specific postfix */
			ASMLabel label = new ASMLabel(funcLabel, true);
			f.headLabelMap.put(LabelUtil.getProvisoPostfix(f.provisosCalls.get(k).provisoMapping), label);
			
			
			/* Generate comment with function name and potential proviso types */
			String com;
			if (f.provisosCalls.get(k).getProvisoPostfix().equals("")) {
				com = "Function: " + f.path;
			}
			else {
				com = ((k == 0)? "Function: " + f.path + ", " : "") + ((f.provisoTypes.isEmpty())? "" : "Provisos: ");
				
				/* Create a String that lists all proviso mappings that this version of the function represents */
				for (int z = k; z < f.provisosCalls.size(); z++) {
					if (f.provisosCalls.get(z).getProvisoPostfix().equals(f.provisosCalls.get(k).getProvisoPostfix())) {
						List<TYPE> types = f.provisosCalls.get(z).provisoMapping;
						
						com += types.stream().map(x -> x.provisoFree().toString()).collect(Collectors.joining(", "));
						
						if (z < f.provisosCalls.size() - 1) com += " | ";
					}
				}
				
				if (com.endsWith(" | ")) com = com.substring(0, com.length() - 3);
			}
			label.comment = new ASMComment(com);
			
			/* Add function label */
			func.instructions.add(label);
			
			/* 
			 * A function may require a R10 reset if it is for example targeted by
			 * interface relay tables. In this case, the always-has-to-be-zero
			 * register R10 is written to another value. So, we have to reset it
			 * here for the next usage.
			 */
			if (f.requireR10Reset)
				func.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(0)));
			
			/* Save FP and lr by default */
			ASMPushStack ipush = new ASMPushStack(new RegOp(REG.FP), new RegOp(REG.LR));
			ipush.optFlags.add(OPT_FLAG.FUNC_CLEAN);
			func.instructions.add(ipush);
			st.push(REG.LR, REG.FP);
			
			ASMVPushStack vpush = new ASMVPushStack();
			vpush.optFlags.add(OPT_FLAG.FUNC_CLEAN);
			func.instructions.add(vpush);
			
			/* Save Stackpointer from caller perspective */
			ASMMov fpMov = new ASMMov(new RegOp(REG.FP), new RegOp(REG.SP));
			ASMMov fpMovBack;
			func.instructions.add(fpMov);
			
			int paramsWithRef = 0;
			
			/* Save parameters in register or stack if the have a address reference */
			for (int i = 0; i < 3; i++) {
				if (r.getReg(i).declaration != null) {
					Declaration d = r.getReg(i).declaration;

					boolean hasRef = false;
					for (Statement s : f.body) 
						hasRef |= Matcher.hasAddressReference(s, d);
					
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
					else func.clearReg(r, st, false, i);
				}
			}
			
			/* Do the same for the VRegSet */
			for (int i = 0; i < 3; i++) {
				if (r.getVRegSet().getReg(i).declaration != null) {
					Declaration d = r.getVRegSet().getReg(i).declaration;
					
					boolean hasRef = false;
					for (Statement s : f.body) 
						hasRef |= Matcher.hasAddressReference(s, d);
					
					if (hasRef) {
						ASMVPushStack init = new ASMVPushStack(new VRegOp(i));
						init.optFlags.add(OPT_FLAG.STRUCT_INIT);
						init.comment = new ASMComment("Push declaration on stack, referenced by addressof.");
						
						/* Only do it if variable was used */
						if (d.last == null || !d.last.equals(d)) {
							func.instructions.add(init);
							paramsWithRef++;
						}
						
						st.push(d);
						r.getVRegSet().free(i);
					}
					else func.clearReg(r, st, true, i);
				}
			}

			/* Cast all statements and add all instructions */
			for (int i = 0; i < f.body.size(); i++) 
				func.loadStatement(f, f.body.get(i), r, map, st);
			
			/* 
			 * This variable records if the frame-pointer FP has been used in any way.
			 * If this is the case, we have to include the FP Register in the Push/Pop Function
			 * Frame to save it when returning to the caller.
			 */
			boolean fpInteraction = false;
			
			/* Check if any instruction reads or writes to the frame-pointer */
			for (ASMInstruction x : func.instructions) {
				if (x.equals(fpMov) || x.equals(ipush)) continue;
				fpInteraction |= ASMOptimizer.overwritesReg(x, REG.FP) || ASMOptimizer.readsReg(x, REG.FP);
			}
				
			/* Check if other function is called within this function */
			boolean hasCall = func.instructions.stream().anyMatch(x -> x instanceof ASMBranch && ((ASMBranch) x).type == BRANCH_TYPE.BL ||
					x instanceof ASMAdd && ((ASMAdd) x).target.reg == REG.LR);
			
			/* Jumplabel to centralized function return */
			ASMLabel funcReturn = new ASMLabel(LabelUtil.getLabel());
			
			/* 
			 * Get a list of all used registers. These registers have to be 
			 * saved in the function frame.
			 */
			List<REG> used = func.getUsed();
			
			
			if (paramsWithRef == 0 && !func.hasParamsInStack() && !f.signals() && !st.newDecsOnStack && f.getReturnType().wordsize() == 1 && !fpInteraction) {
				func.instructions.remove(fpMov);
				ipush.operands.remove(0);
			}
			
			if (!hasCall && !func.hasParamsInStack() && f.getReturnType().wordsize() == 1 && !f.signals() && paramsWithRef == 0) {
				if (used.isEmpty()) {
					func.instructions.remove(ipush);
					func.instructions.remove(vpush);
				}
				else {
					/* Patch used registers into push instruction at the start */
					ipush.operands.clear();
					vpush.operands.clear();
					
					used.forEach(x -> {
						if (x.toInt() < 16) ipush.operands.add(new RegOp(x));
						else vpush.operands.add(new VRegOp(x));
					});
					
					func.patchBxToB(funcReturn);
				}		
			}
			else {
				/* Patch used registers into push instruction at the start */
				ipush.operands.clear();
				vpush.operands.clear();
				
				used.forEach(x -> {
					if (x.toInt() < 16) ipush.operands.add(new RegOp(x));
					else vpush.operands.add(new VRegOp(x));
				});
				
				if (func.hasParamsInStack() || f.signals() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || fpInteraction) ipush.operands.add(new RegOp(REG.FP));
				
				if (hasCall) ipush.operands.add(new RegOp(REG.LR));
				
				func.patchBxToB(funcReturn);
			}
			
			/* 
			 * Main specific removals, need to remove from push here already since
			 * FP-relativ addressing is patched down below and is dependent on pushed
			 * regs.
			 */
			if (f.path.build().equals("main")) {
				for (int i = 0; i < ipush.operands.size(); i++) {
					if (ipush.operands.get(i).reg != REG.FP && ipush.operands.get(i).reg != REG.LR) {
						ipush.operands.remove(i);
						i--;
					}
				}
				
				vpush.operands.clear();
			}
			
			/* Patch offset based on amount of pushed registers excluding LR and FP */
			func.patchFramePointerAddressing((ipush.operands.size() + vpush.operands.size()) * 4);
			
			/* If function signals() exceptions, add escape target for exceptions to jump to */
			if (f.signals()) func.instructions.add(func.copyLoopEscape);
			
			ASMPopStack ipop = null;
			ASMVPopStack vpop = null;
			
			if (paramsWithRef > 0 || hasCall || func.hasParamsInStack() || !used.isEmpty() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || f.signals()) {
				/* Add centralized stack reset and register restoring */
				func.instructions.add(funcReturn);
				
				/* Check if exception was thrown */
				if (f.signals()) func.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(0)));
				
				ipop = new ASMPopStack();
				vpop = new ASMVPopStack();
				
				for (REG reg : used) {
					if (reg.toInt() < 16) ipop.operands.add(new RegOp(reg));
					else vpop.operands.add(new VRegOp(reg));
				}
				
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
						if (paramsWithRef > 0 || func.hasParamsInStack() || f.signals() || st.newDecsOnStack || f.getReturnType().wordsize() > 1 || fpInteraction) ipop.operands.add(new RegOp(REG.FP));
						
						/* Function has a call, must save LR */
						if (hasCall) ipop.operands.add(new RegOp(REG.LR));
					}
				}
				
				/* Set relation for optimizer */
				vpush.popCounterpart = vpop;
				vpop.optFlags.add(OPT_FLAG.FUNC_CLEAN);
				func.instructions.add(vpop);
				
				/* Set relation for optimizer */
				ipush.popCounterpart = ipop;
				ipop.optFlags.add(OPT_FLAG.FUNC_CLEAN);
				func.instructions.add(ipop);
			}
			
			int size = 0;
			for (Pair<Declaration, Integer> p  : func.parameterMapping) {
				if (p.getSecond() == -1) {
					/* Stack shrinks by parameter word size */
					size += p.getFirst().getType().wordsize();
				}
			}
			
			if (size != 0 && !f.path.build().equals("main")) 
				func.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(size * 4)));
		
			
			ASMLabel singleWordSkip = new ASMLabel(LabelUtil.getLabel());
			if (f.getReturnType().wordsize() == 1 && f.signals()) 
				func.instructions.add(new ASMBranch(BRANCH_TYPE.B, COND.EQ, new LabelOp(singleWordSkip)));
			
			
			if (f.getReturnType().wordsize() > 1 || f.signals()) {
				if (f.signals() && f.getReturnType().wordsize() > 1) {
					/* 
					 * No exception, move word size of return type in R0, if execption 
					 * were thrown, the word size would already be in R0 
					 */
					ASMMov mov = new ASMMov(new RegOp(REG.R0), new ImmOp(f.getReturnType().wordsize() * 4), COND.EQ);
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
			
			if (f.getReturnType().wordsize() == 1 && f.signals()) 
				func.instructions.add(singleWordSkip);
			
			/* Branch back */
			func.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOp(REG.LR)));

			/* Main specific removals */
			if (f.path.build().equals("main") && ipop != null) {
				for (int i = 0; i < ipop.operands.size(); i++) {
					if (ipop.operands.get(i).reg != REG.FP && ipop.operands.get(i).reg != REG.LR) {
						ipop.operands.remove(i);
						i--;
					}
				}
				
				vpop.operands.clear();
			}
			
			/* Clear Reg-Frame if empty */
			if (ipush.operands.isEmpty()) {
				func.instructions.remove(ipush);
				func.instructions.remove(ipop);
			}
			
			/* Clear VFP-Reg-Frame if empty */
			if (vpush.operands.isEmpty()) {
				func.instructions.remove(vpush);
				func.instructions.remove(vpop);
			}
			
			if (f.provisosCalls.size() > 1 && k < f.provisosCalls.size() - 1) 
				func.instructions.add(new ASMSeperator());
			
			if (!f.provisoTypes.isEmpty()) {
				all.addAll(func.instructions);
				func.instructions.clear();
			}
			
			LabelUtil.currentContext = null;
		}
		
		if (!f.provisoTypes.isEmpty()) func.instructions.addAll(all);
	
		func.registerMetric();
		return func;
	}
	
	/**
	 * Replace bx lr with branch to function end for centralized stack reset and bx
	 */
	public void patchBxToB(ASMLabel funcReturn) {
		for (ASMInstruction instruction : this.instructions) {
			if (instruction instanceof ASMBranch branch) {
				/* Only patch bx and instructions that are not part of exceptional exit */
				if (branch.type == BRANCH_TYPE.BX && !branch.optFlags.contains(OPT_FLAG.EXC_EXIT)) {
					branch.type = BRANCH_TYPE.B;
					branch.optFlags.add(OPT_FLAG.BRANCH_TO_EXIT);
					branch.target = new LabelOp(funcReturn);
				}
			}
		}
	}
	
	public void patchFramePointerAddressing(int offset) throws CGEN_EXC {
		for (ASMInstruction ins : this.instructions) {
			if (ins instanceof ASMStackOp stackOp) {
				if (stackOp.op0 != null && stackOp.op0.reg == REG.FP) {
					if (stackOp.op1 instanceof PatchableImmOp op) {

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
			else if (ins instanceof ASMBinaryData binary) {

				if (binary.op0 != null && binary.op0.reg == REG.FP) {
					if (binary.op1 instanceof PatchableImmOp op) {
						if (op.dir == PATCH_DIR.UP) {
							op.patch(offset);
							binary.op1 = new ImmOp(op.patchedValue, op);
						}
					}
				}
			}
			else if (ins instanceof ASMMemOp mem) {
				if (mem.op0 instanceof RegOp && ((RegOp) mem.op0).reg == REG.FP) {
					if (mem.op1 instanceof PatchableImmOp op) {
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
		return this.parameterMapping.stream().map(Pair::getSecond).anyMatch(x -> x == -1);
	}
	
	/**
	 * Return a list of all registers that were used. Registers 0-2 and 10-15 are excluded.
	 */
	public List<REG> getUsed() {
		List<REG> used = new ArrayList();
		
		this.instructions.forEach(x -> {
			if (x instanceof ASMMov) {
				REG reg = ((ASMMov) x).target.reg;
				
				boolean include = !reg.isOperandReg() && !reg.isSpecialReg();
				if (include && !used.contains(reg)) used.add(reg);
			}
		});
		
		return used;
	}
	
	/**
	 * Create a parameter mapping that assigns each parameter either a register or the stack
	 * as a location.
	 */
	public List<Pair<Declaration, Integer>> getParameterMapping() {
		int r = 0;
		int s = 0;
		
		List<Pair<Declaration, Integer>> mapping = new ArrayList();
		
		for (Declaration dec : this.source.parameters) {
			if (dec.getType().wordsize() == 1 && !dec.getType().isStruct()) {
				if (dec.getType().isFloat() && s < 3) {
					/* Load in Sx register */
					mapping.add(new Pair(dec, 16 + s++));
					continue;
				}
				else if (r < 3) {
					/* Load in Rx register */
					mapping.add(new Pair(dec, r++));
					continue;
				}
			}
			 
			/* Load in stack */
			mapping.add(new Pair(dec, -1));
		}
		
		return mapping;
	}
	
} 
