package Opt.ASM;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Exc.SNIPS_EXC;
import Imm.ASM.ASMHardcode;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Directive.ASMDirective;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMMemBlock;
import Imm.ASM.Memory.ASMMemBlock.MEM_BLOCK_MODE;
import Imm.ASM.Memory.ASMMemOp;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Processing.ASMUnaryData;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMla;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMMvn;
import Imm.ASM.Processing.Arith.ASMRsb;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Shift;
import Imm.ASM.Util.Shift.SHIFT;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.VFP.Memory.ASMVLdr;
import Imm.ASM.VFP.Memory.ASMVLdrLabel;
import Imm.ASM.VFP.Processing.Arith.ASMVCvt;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

/**
 * This optimizer can simplify a sequence of asm instructions. By doing so
 * the optimizer will not change the behaviour of the program itself.<br>
 * The optimizer follows an internal logic of the Compiler, so this optimizer
 * will not work for any Assembly program, since it may violate the compiler internal
 * logic. This may lead to context changing optimizations.
 */
public class ASMOptimizer {

			/* ---< FIELDS >--- */
	/** Wether an optimization was done in this cycle and a new iteration should be launched. */
	boolean OPT_DONE = true;

	int iterations = 0;
	long opts = 0;
	
			/* ---< METHODS >--- */
	public void OPT_DONE() {
		this.OPT_DONE = true;
		opts++;
	}
	
	/** Optimize given body. */
	public double optimize(List<ASMInstruction> ins, boolean isMainFile) {
		double before = ins.size();
		
		ProgressMessage aopt_progress = null;
		if (isMainFile) aopt_progress = new ProgressMessage("OPT1 -> Starting", 30, LogPoint.Type.INFO);
		
		/* While an optimization was done in this iteration */
		while (OPT_DONE) {
			iterations++;
			
			OPT_DONE = false;
			
			/**
			 * add rx, ry, #2
			 * add r0, rx, #4
			 * Replace with:
			 * add r0, ry, #6
			 */
			this.defragmentAdditions(ins);
			
			/**
			 * push { r0 }
			 * push { r1 }
			 * Replace with:
			 * push { r1, r0 }
			 */
			this.defragmentPush(ins);
			
			/**
			 * sub r0, fp, #8
			 * add r0, r0, #4
			 * Replace with:
			 * sub r0, fp, #4
			 */
			this.defragmentDeltas(ins);
			
			/**
			 * mov r0, #10
			 * add r0, r0, r2
			 * Replace with:
			 * add r0, r2, #10
			 */
			this.additionCommutative(ins);
			
			/**
			 * mov r0, #10
			 * sub r0, r0, r2
			 * Replace with:
			 * rsb r0, r2, #10
			 */
			this.subtractionSemiCommutative(ins);
			
			/**
			 * add r0, r1, r2
			 * mov r4, r0
			 * Replace with:
			 * add r4, r1, r2
			 */
			this.removeExpressionIndirectTargeting(ins);
			
			/**
			 * ldr r0, [r1]
			 * mov r4, r0
			 * Replace with:
			 * add r4, [r1]
			 */
			this.removeLdrIndirectTargeting(ins);
			
			/**
			 * mov r0, rx
			 * cmp rx, r1
			 * Replace with:
			 * cmp rx, r1
			 */
			this.removeOperandIndirectTargeting(ins);
			
			/**
			 * Substitute immediate values after assignment
			 * to register into other instructions.
			 */
			this.constantOperandPropagation(ins);
			
			/**
			 * b .L1
			 * ... <- Remove these Lines until Label or newline
			 */
			this.clearInstructionsAfterBranch(ins);
			
			/**
			 * Removes non-referenced Labels
			 */
			this.clearUnusedLabels(ins);
			
			/**
			 * mov r1, r0
			 * mov r0, r1 <-- Delete this line
			 */
			this.removeDoubleCrossing(ins);
			
			/**
			 * .L0:
			 * .L1:
			 * Replace with:
			 * .L1   <- Rename all .L0 occurrenced to this label
			 */
			this.removeDoubleLabels(ins);
			
			/**
			 * b .Lx <-- Remove this line
			 * .Lx: 
			 */
			this.removeBranchesBeforeLabelToLabel(ins);
			
			/**
			 * push { r0 }
			 * ... <- Does not reassign r0
			 * pop { r0 }
			 */
			this.removeUnnessesaryPushPop(ins);
			
			/**
			 * mov r0, rx
			 * push { r0 }
			 * Replace with:
			 * push { rx }
			 */
			this.removeImplicitPushPopTargeting(ins);
			
			/**
			 * push { r0 }
			 * ... <- Does not reassign r1
			 * pop { r1 }
			 * 
			 * Transform to
			 * mov r1, r0
			 * ... <- Does not reassign r1
			 * ---
			 */
			this.removeIndirectPushPopAssign(ins);
			
			/**
			 * add r0, r0, #0 <- Remove this line
			 */
			this.removeZeroInstruction(ins);
			
			/**
			 * instruction a <- Assigns to rx
			 * ... <- Do not reassign or read rx
			 * instruction b <- Assigns to rx
			 * 
			 * -> Remove instruction 
			 */
			this.removeUnusedAssignment(ins);
			
			/**
			 * Removes mov rx, ... instructions, where rx = r3 - r9,
			 * and rx is not being used until a bx lr
			 */
			this.removeUnusedRegistersStrict(ins);
			
			/**
			 * Attempt to collect immediate operands before a mult,
			 * and try to precalculate the mult based on the operands.
			 */
			this.multPrecalc(ins);
			
			this.removeMovId(ins);
			
			/**
			 * lsl r0, r1, #0
			 * Replace with:
			 * mov r0, r1
			 */
			this.shiftBy0IsMov(ins);
			
			if (!OPT_DONE) {
				/**
				 * add r0, fp, #4
				 * ldr r0, [r0]
				 * Replace with:
				 * ldr r0, [fp, #4]
				 */
				this.removeIndirectMemOpAddressing(ins);
			}
			
			/**
			 * Execute these routines when no other optimization can be made.
			 * Executing this routine to early can block data-paths that could
			 * under other circumstances be simplified.
			 */
			if (!OPT_DONE) {
				/**
				 * Attempts to move pop operations further up to allow 
				 * for more data-path analysis. This will result in push and pop
				 * operation positions to be closer together, which makes it easier
				 * to potentially remove/simplify them.
				 * 
				 * This routine can - in some cases - prevent other optimizations
				 * from working out well. But - seeing the average numbers - this
				 * routine should improve the overall assembly.
				 */
				this.pushPopRelocation(ins);
			}
			
			if (!OPT_DONE) {
				/**
				 * Checks through all function bodies and removes the pushed/popped regs 
				 * at the function start/end, which are strictly not used in the function
				 * body.
				 */
				this.removeFuncCleanStrict(ins);
			}
			
			if (!OPT_DONE) {
				/**
				 * Checks if function body only contains one bl-Branch and if the branch is
				 * directley before the pop func clean. If yes, remove the lr push/pop and
				 * set the branch type to b. This means the branched function will return to
				 * the original caller.
				 */
				this.implicitReturn(ins);
			}
			
			if (!OPT_DONE) {
				/**
				 * This operation is potentially dangerous and is still WIP. For the current
				 * tests the operation works, but when using ASM Injection etc. the operation
				 * could fail spectaculary.
				 * 
				 * The operation attempts to remove the fp/sp exchange and fp func clean. To do
				 * this, the operation subsitutes the sp to every occurrence of the fp and modifies
				 * offsets.
				 */
				this.removeFPExchange(ins);
			}
			
			if (!OPT_DONE) {

				this.mulToMlaMerge(ins);
				
			}
			
			if (!OPT_DONE) {
				
				this.bxSubstitution(ins);
				
			}
			
			if (!OPT_DONE) {

				this.removeStrOperandIndirectTargeting(ins);
				
			}
			
			if (!OPT_DONE) {
				
				this.pushDownInsDataUsed(ins);
				
			}
			
			if (!OPT_DONE) {
				
				this.deepRegPropagation(ins);
				
			}
			
			if (!OPT_DONE) {
				
				/**
				 * bne L3
				 * b L2	    -> beq L2
				 * L3:
				 */
				this.clearEmptyBreakStatements(ins);
				
			}
			
			if (!OPT_DONE) {
				
				this.removeSameValueAssignments(ins);
				
			}
			
		}
		
		/**
		 * Check if a bx lr statement is directley after the func clean pop. If the last
		 * pop operand is a lr, substitute pc in the operand and remove the bx lr.
		 */
		this.popReturnDirect(ins);
		
		/**
		 * Attempt to push down the Function Frame Push. If this frame Push is pushed below
		 * the base-cases of recursive functions, the Frame-Push for these Base-Cases can
		 * be left out, saving an increased amount of instructions based on the size of
		 * the recursion-tree. This optimization rarely does improve anything, and often
		 * discards the changes it does.
		 */
		if (CompilerDriver.useExperimentalOptimizer) this.pushDownFunctionFrame(ins);
		
		/* Finishing touches, no iteration required */
		this.popPcSubstitution(ins);
		
		/* Remove labels that are not referenced anywhere. */
		this.clearUnusedLabels(ins);
		
		/* Replace sub-structure loads with ldm */
		this.replaceR0R1R2LdrWithLdm(ins);
		
		/* Replace large push/pop operations with ldm/stm */
		this.replacePushPopWithBlockMemory(ins);
		
		/* Remove #0 operands from ldr and str instructions */
		this.removeZeroOperands(ins);
		
		/* Filter duplicate empty lines */
		if (ins.size() > 1) {
			for (int i = 1; i < ins.size(); i++) {
				if (ins.get(i - 1) instanceof ASMSeperator && ins.get(i) instanceof ASMSeperator) {
					ins.remove(i);
					i--;
				}
			}
		}
		
		if (isMainFile) aopt_progress.finish();
		return Math.round(1 / (before / 100) * (before - ins.size()) * 100) / 100;
	}
	
	public void replaceR0R1R2LdrWithLdm(List<ASMInstruction> ins0) {
		for (int i = 2; i < ins0.size(); i++) {
			if (ins0.get(i - 2) instanceof ASMLdr) {
				ASMLdr ldr0 = (ASMLdr) ins0.get(i - 2);
				
				if (ldr0.op0 instanceof RegOp && ldr0.op1 != null && ldr0.op1 instanceof ImmOp && ldr0.cond == null) { 
				
					REG base = ((RegOp) ldr0.op0).reg;
					
					if (base == REG.SP || base == REG.FP) {
						List<ASMLdr> ldrs = new ArrayList();
						ldrs.add(ldr0);
						
						for (int a = i - 1; a <= i; a++) {
							if (ins0.get(a).cond != null) break;
							if (ins0.get(a) instanceof ASMLdr) {
								ASMLdr ldr = (ASMLdr) ins0.get(a);
								
								if (ldr.op0 instanceof RegOp && ((RegOp) ldr.op0).reg == base && ldr.op1 != null && ldr.op1 instanceof ImmOp) {
									ldrs.add(ldr);
								}
								else break;
							}
							else break;
						}
						
						if (ldrs.size() > 2) {
							/* Check that all immediates are back-to-back */
							boolean clear = true;
							for (int a = 1; a < ldrs.size(); a++) {
								clear &= ((ImmOp) ldrs.get(a - 1).op1).value - 4 == ((ImmOp) ldrs.get(a).op1).value;
							}
							
							int start = ((ImmOp) ldrs.get(0).op1).value;
							
							if (clear && Math.abs(start) < 256 && start != 0) {
								
								List<RegOp> regs = new ArrayList();
								
								for (int a = 0; a < ldrs.size(); a++) 
									regs.add(ldrs.get(a).target);
								
								for (int a = 0; a < ldrs.size(); a++) 
									ins0.remove(i - 2);
								
								if (start < 0) 
									ins0.add(i - 2, new ASMSub(new RegOp(REG.R0), new RegOp(base), new ImmOp(-start)));
								else 
									ins0.add(i - 2, new ASMAdd(new RegOp(REG.R0), new RegOp(base), new ImmOp(start)));
									
								ASMMemBlock block = new ASMMemBlock(MEM_BLOCK_MODE.LDMFA, false, new RegOp(REG.R0), regs, null);
							
								ins0.add(i - 1, block);
							}
						}
					}
				}
			}
		}
	}
	
	public void removeZeroOperands(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			if (ins instanceof ASMMemOp) {
				ASMMemOp op = (ASMMemOp) ins;
				if (op.op1 != null && op.op1 instanceof ImmOp) {
					if (((ImmOp) op.op1).value == 0) {
						op.op1 = null;
					}
				}
			}
			else if (ins instanceof ASMStackOp) {
				ASMStackOp op = (ASMStackOp) ins;
				if (op.op1 != null && op.op1 instanceof ImmOp) {
					if (((ImmOp) op.op1).value == 0) {
						op.op1 = null;
					}
				}
			}
		}
	}
	
	public void replacePushPopWithBlockMemory(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				if (ASMMemBlock.checkInOrder(push.operands) && push.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
					ins0.set(i, new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(push.operands.size() * 4)));
					
					ASMMemBlock block = new ASMMemBlock(MEM_BLOCK_MODE.STMEA, false, new RegOp(REG.SP), push.operands, push.cond);
					ins0.add(i + 1, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOp> ops = new ArrayList();
					for (RegOp r : push.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && push.operands.size() > 1) {
						ASMMemBlock block = new ASMMemBlock(MEM_BLOCK_MODE.STMFD, true, new RegOp(REG.SP), ops, push.cond);
						ins0.set(i, block);
					}
				}
			}
			else if (ins0.get(i) instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) ins0.get(i);
				
				if (ASMMemBlock.checkInOrder(pop.operands) && pop.operands.size() > 1) {
					ASMMemBlock block = new ASMMemBlock(MEM_BLOCK_MODE.LDMFD, true, new RegOp(REG.SP), pop.operands, pop.cond);
					ins0.set(i, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOp> ops = new ArrayList();
					for (RegOp r : pop.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && pop.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
						ins0.set(i, new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(ops.size() * 4)));
						
						ASMMemBlock block = new ASMMemBlock(MEM_BLOCK_MODE.LDMEA, false, new RegOp(REG.SP), ops, pop.cond);
						ins0.add(i + 1, block);
					}
				}
			}
		}
	}
	
	/**
	 * Check if given register is overwritten by given instruction.
	 */
	public static boolean overwritesReg(ASMInstruction ins, REG reg) {
		if (ins instanceof ASMBinaryData) {
			ASMBinaryData data = (ASMBinaryData) ins;
			return data.target.reg == reg;
		}
		else if (ins instanceof ASMMult) {
			ASMMult mul = (ASMMult) ins;
			return mul.target.reg == reg;
		}
		else if (ins instanceof ASMMla) {
			ASMMla mla = (ASMMla) ins;
			return mla.target.reg == reg;
		}
		else if (ins instanceof ASMMemOp) {
			ASMMemOp memOp = (ASMMemOp) ins;
			return memOp.target.reg == reg && memOp instanceof ASMLdr;
		}
		else if (ins instanceof ASMLdrStack) {
			ASMLdrStack load = (ASMLdrStack) ins;
			return load.target.reg == reg;
		}
		else if (ins instanceof ASMStrStack) {
			ASMStrStack load = (ASMStrStack) ins;
			return (load.memOp == MEM_OP.POST_WRITEBACK || load.memOp == MEM_OP.PRE_WRITEBACK) && (load.op1 instanceof RegOp && ((RegOp) load.op1).reg == reg);
		}
		else if (ins instanceof ASMPopStack) {
			ASMPopStack pop = (ASMPopStack) ins;
			return pop.operands.stream().filter(x -> x.reg == reg).count() > 0;
		}
		else if (ins instanceof ASMPushStack || ins instanceof ASMLabel || ins instanceof ASMComment || 
				 ins instanceof ASMSeperator || ins instanceof ASMBranch || ins instanceof ASMCmp) {
			return false;
		}
		else if (ins instanceof ASMHardcode) {
			/* Better safe than sorry */
			return true;
		}
		else if (ins instanceof ASMDirective) {
			return false;
		}
		else throw new SNIPS_EXC("Cannot check if instruction overwrites register: " + ins.getClass().getName());
	}
	
	/**
	 * Check if given register is read by given instruction.
	 */
	public static boolean readsReg(ASMInstruction ins, REG reg) {
		if (ins instanceof ASMBinaryData) {
			ASMBinaryData data = (ASMBinaryData) ins;
			return (data.op0 != null && data.op0.reg == reg) || (data.op1 instanceof RegOp && ((RegOp) data.op1).reg == reg);
		}
		else if (ins instanceof ASMCmp) {
			ASMCmp cmp = (ASMCmp) ins;
			return cmp.op0.reg == reg || (cmp.op1 instanceof RegOp && ((RegOp) cmp.op1).reg == reg);
		}
		else if (ins instanceof ASMStr) {
			ASMStr str = (ASMStr) ins;
			return str.target.reg == reg || (str.op0 instanceof RegOp && ((RegOp) str.op0).reg == reg) || (str.op1 instanceof RegOp && ((RegOp) str.op1).reg == reg);
		}
		else if (ins instanceof ASMMemOp) {
			ASMMemOp op = (ASMMemOp) ins;
			return (op.op0 instanceof RegOp && ((RegOp) op.op0).reg == reg) || (op.op1 instanceof RegOp && ((RegOp) op.op1).reg == reg);
		}
		else if (ins instanceof ASMLdrStack) {
			ASMLdrStack ldr = (ASMLdrStack) ins;
			return (ldr.op0 instanceof RegOp && ((RegOp) ldr.op0).reg == reg) || (ldr.op1 instanceof RegOp && ((RegOp) ldr.op1).reg == reg);
		}
		else if (ins instanceof ASMMult) {
			ASMMult mul = (ASMMult) ins;
			return mul.op0.reg == reg || mul.op1.reg == reg;
		}
		else if (ins instanceof ASMMla) {
			ASMMla mla = (ASMMla) ins;
			return mla.op0.reg == reg || mla.op1.reg == reg || mla.op2.reg == reg;
		}
		else if (ins instanceof ASMStrStack) {
			ASMStrStack str = (ASMStrStack) ins;
			return str.target.reg == reg || str.op0.reg == reg || (str.op1 != null && str.op1 instanceof RegOp && ((RegOp) str.op1).reg == reg);
		}
		else if (ins instanceof ASMPushStack) {
			ASMPushStack push = (ASMPushStack) ins;
			for (RegOp r : push.operands) if (r.reg == reg) return true;
			return false;
		}
		else if (ins instanceof ASMLabel || ins instanceof ASMComment || 
				 ins instanceof ASMSeperator || ins instanceof ASMBranch ||
				 ins instanceof ASMPopStack) {
			return false;
		}
		else if (ins instanceof ASMHardcode) {
			/* Better safe than sorry */
			return true;
		}
		else if (ins instanceof ASMDirective) {
			return false;
		}
		else throw new SNIPS_EXC("Cannot check if instruction reads register: " + ins.getClass().getName());
	}
	
	public int countPushedWords(List<ASMInstruction> patch, ASMInstruction end) {
		int pushed = 0;
		for (ASMInstruction ins : patch) {
			if (ins.optFlags.contains(OPT_FLAG.LOOP_BREAK_RESET)) continue;
			
			if (ins.equals(end)) break;
			if (ins instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins;
				pushed += push.operands.size();
			}
			else if (ins instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) ins;
				pushed -= pop.operands.size();
			}
			else if (ins instanceof ASMAdd) {
				ASMAdd add = (ASMAdd) ins;
				if (add.target.reg == REG.SP && add.target.reg == REG.SP && add.op1 instanceof ImmOp) {
					pushed -= ((ImmOp) add.op1).value >> 2;
				}
			}
			else if (ins instanceof ASMSub) {
				ASMSub sub = (ASMSub) ins;
				if (sub.target.reg == REG.SP && sub.target.reg == REG.SP && sub.op1 instanceof ImmOp) {
					pushed += ((ImmOp) sub.op1).value >> 2;
				}
			}
			else if (ins instanceof ASMMemOp) {
				
			}
			else if (ins instanceof ASMStackOp && (((ASMStackOp) ins).memOp == MEM_OP.PRE_NO_WRITEBACK || ((ASMStackOp) ins).op1 == null)) {
				
			}
			else {
				if (readsReg(ins, REG.SP) || overwritesReg(ins, REG.SP)) {
					throw new SNIPS_EXC("Cannot SP info from: " + ins.getClass().getName());
				}
			}
		}
		
		return pushed;
	}
	
	public void patchFPtoSP(List<ASMInstruction> patch) {
		for (ASMInstruction ins : patch) {
			if (ins instanceof ASMBinaryData) {
				ASMBinaryData d = (ASMBinaryData) ins;
				if (d.target.reg == REG.FP) d.target.reg = REG.SP;
				if (d.op0.reg == REG.FP) d.op0.reg = REG.SP;
				if (d.op1 instanceof RegOp) {
					RegOp op = (RegOp) d.op1;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMCmp) {
				ASMCmp d = (ASMCmp) ins;
				if (d.op0.reg == REG.FP) d.op0.reg = REG.SP;
				if (d.op1 instanceof RegOp) {
					RegOp op = (RegOp) d.op1;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMBranch) {
				ASMBranch d = (ASMBranch) ins;
				if (d.target instanceof RegOp) {
					RegOp op = (RegOp) d.target;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp memOp = (ASMMemOp) ins;
				if (memOp.target.reg == REG.FP) memOp.target.reg = REG.SP;
				
				if (memOp.op1 instanceof PatchableImmOp) {
					PatchableImmOp op = (PatchableImmOp) memOp.op1;
					int words = this.countPushedWords(patch, memOp);
					memOp.op1 = new ImmOp(op.patchedValue + (words * 4));
				}
				else if (memOp.op1 instanceof ImmOp && memOp.op0 instanceof RegOp && ((RegOp) memOp.op0).reg == REG.FP) {
					ImmOp op = (ImmOp) memOp.op1;
					int words = this.countPushedWords(patch, memOp);
					memOp.op1 = new ImmOp(op.value + (words * 4));
				}
				
				if (memOp.op0 instanceof RegOp) {
					RegOp op = (RegOp) memOp.op0;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
				if (memOp.op1 instanceof RegOp) {
					RegOp op = (RegOp) memOp.op1;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMLdrStack) {
				ASMLdrStack ldr = (ASMLdrStack) ins;
				if (ldr.target.reg == REG.FP) ldr.target.reg = REG.SP;
				
				if (ldr.op1 instanceof PatchableImmOp) {
					PatchableImmOp op = (PatchableImmOp) ldr.op1;
					int words = this.countPushedWords(patch, ldr);
					ldr.op1 = new ImmOp(op.patchedValue + (words * 4));
				}
				else if (ldr.op1 instanceof ImmOp && ldr.op0 instanceof RegOp && ((RegOp) ldr.op0).reg == REG.FP) {
					ImmOp op = (ImmOp) ldr.op1;
					int words = this.countPushedWords(patch, ldr);
					ldr.op1 = new ImmOp(op.value + (words * 4));
				}
				
				if (ldr.op0 instanceof RegOp) {
					RegOp op = (RegOp) ldr.op0;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
				if (ldr.op1 instanceof RegOp) {
					RegOp op = (RegOp) ldr.op1;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMMult) {
				ASMMult mul = (ASMMult) ins;
				if (mul.target.reg == REG.FP) mul.target.reg = REG.SP;
				if (mul.op0.reg == REG.FP) mul.op0.reg = REG.SP;
				if (mul.op1.reg == REG.FP) mul.op1.reg = REG.SP;
			}
			else if (ins instanceof ASMStrStack) {
				ASMStrStack str = (ASMStrStack) ins;
				if (str.target.reg == REG.FP) str.target.reg = REG.SP;
				
				if (str.op1 instanceof PatchableImmOp) {
					PatchableImmOp op = (PatchableImmOp) str.op1;
					int words = this.countPushedWords(patch, str);
					str.op1 = new ImmOp(op.patchedValue + (words * 4));
				}
				else if (str.op1 instanceof ImmOp && str.op0 instanceof RegOp && ((RegOp) str.op0).reg == REG.FP) {
					ImmOp op = (ImmOp) str.op1;
					int words = this.countPushedWords(patch, str);
					str.op1 = new ImmOp(op.value + (words * 4));
				}
				
				if (str.op0 instanceof RegOp) {
					RegOp op = (RegOp) str.op0;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
				if (str.op1 instanceof RegOp) {
					RegOp op = (RegOp) str.op1;
					if (op.reg == REG.FP) op.reg = REG.SP;
				}
			}
			else if (ins instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins;
				for (RegOp r : push.operands) if (r.reg == REG.FP) r.reg = REG.SP;
			}
			else if (ins instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) ins;
				for (RegOp r : pop.operands) if (r.reg == REG.FP) r.reg = REG.SP;
			}
			else if (ins instanceof ASMLabel || ins instanceof ASMComment || ins instanceof ASMSeperator) {
				
			}
			else throw new RuntimeException("OPT: Cannot patch FP to SP: " + ins.getClass().getName());
		}
	}
	
	public void pushDownInsDataUsed(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			
			if (ins instanceof ASMBranch || ins instanceof ASMPushStack || ins instanceof ASMPopStack || 
				ins instanceof ASMLdr || ins instanceof ASMLdrStack) continue;
			
			for (REG reg : REG.values()) {
				if (reg.isSpecialReg()) continue;
				
				if (ASMOptimizer.overwritesReg(ins, reg)) {
					for (int z = i + 1; z < ins0.size(); z++) {
						ASMInstruction ins1 = ins0.get(z);
						
						/*
						 * To prevent infinite optimization cycles, break when the ins1 instruction
						 * overwrites a register that is less or equal to the register overwritten by 
						 * the ins instruction. This will order the assigned registers in register
						 * number descending, eventually no more pushdown can be done.
						 */
						boolean skip = false;
						for (REG reg0 : REG.values()) {
							if (reg0.isSpecialReg()) continue;
							if (ASMOptimizer.overwritesReg(ins1, reg0) && (reg0.toInt() <= reg.toInt() || ASMOptimizer.readsReg(ins, reg0))) {
								skip = true;
							}
						}
						
						/* Cannot pushdown, essential for addressing */
						if (ASMOptimizer.overwritesReg(ins1, REG.SP) || ASMOptimizer.readsReg(ins1, REG.SP) ||
							ASMOptimizer.overwritesReg(ins1, REG.FP) || ASMOptimizer.readsReg(ins1, REG.FP)) break;
						
						/* Cannot pushdown, essential for program flow */
						if (ASMOptimizer.overwritesReg(ins1, REG.PC) || ASMOptimizer.readsReg(ins1, REG.PC)) break;
						
						boolean overwritesCondition = false;
						overwritesCondition |= ins1 instanceof ASMUnaryData && ((ASMUnaryData) ins1).updateConditionField;
						overwritesCondition |= ins1 instanceof ASMBinaryData && ((ASMBinaryData) ins1).updateConditionField;
						
						if (ins1 instanceof ASMLabel || ins1 instanceof ASMBranch || 
							ins1 instanceof ASMPushStack || ins1 instanceof ASMPopStack || 
							ins1 instanceof ASMCmp || overwritesCondition || skip) break;
						
						if (!ASMOptimizer.readsReg(ins1, reg)) {
							ins0.remove(z);
							ins0.add(z - 1, ins1);
							OPT_DONE();
						}
						else break;
					}
				}
			}
		}
	}
	
	public void deepRegPropagation(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i);
				
				if (mov.optFlags.contains(OPT_FLAG.WRITEBACK) || mov.target.reg == REG.PC) continue;
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
				
					if (reg.isSpecialReg() || mov.target.reg.isSpecialReg()) continue;

					boolean remove = true;
					
					for (int a = i + 1; a < ins0.size(); a++) {
						ASMInstruction ins = ins0.get(a);
						
						if (ins instanceof ASMBranch || ins instanceof ASMLabel || (ins instanceof ASMMov && ((ASMMov) ins).target.reg == REG.PC)) {
							remove = false;
							break;
						}
						
						if (ins instanceof ASMBinaryData) {
							ASMBinaryData d = (ASMBinaryData) ins;
							
							if (d.op0 != null && d.op0.reg == mov.target.reg && d.op0.reg != reg) {
								d.op0.reg = reg;
								OPT_DONE();
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									OPT_DONE();
								}
							}
						}
						else if (ins instanceof ASMCmp) {
							ASMCmp d = (ASMCmp) ins;
							
							if (d.op0.reg == mov.target.reg) {
								d.op0.reg = reg;
								OPT_DONE();
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									OPT_DONE();
								}
							}
						}
						else if (ins instanceof ASMStr) {
							ASMStr d = (ASMStr) ins;
							
							if (d.target.reg == mov.target.reg) {
								d.target.reg = reg;
								OPT_DONE();
							}
							if (d.op0 instanceof RegOp) {
								RegOp op = (RegOp) d.op0;
								if (op.reg == mov.target.reg && op.reg != reg) {
									op.reg = reg;
									OPT_DONE();
								}
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									OPT_DONE();
								}
							}
						}
						else {
							remove = false;
						}
						
						if (overwritesReg(ins, mov.target.reg) || overwritesReg(ins, reg)) {
							remove = false;
							break;
						}
					}
					
					if (remove) {
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	public void clearEmptyBreakStatements(List<ASMInstruction> ins0) {
		for (int i = 2; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMLabel && ins0.get(i - 1) instanceof ASMBranch && ins0.get(i - 2) instanceof ASMBranch) {
				ASMBranch branch0 = (ASMBranch) ins0.get(i - 2);
				ASMBranch branch1 = (ASMBranch) ins0.get(i - 1);
				ASMLabel label = (ASMLabel) ins0.get(i);
				
				if (branch0.cond != null && branch1.cond == null) {
					COND cond = branch0.cond;
					
					if (branch0.target instanceof LabelOp && ((LabelOp) branch0.target).label.equals(label)) {
						branch1.cond = cond.negate();
						ins0.remove(i - 2);
						OPT_DONE();
						i--;
					}
				}
			}
		}
	}
	
	public void removeSameValueAssignments(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			
			if (ins instanceof ASMPushStack || ins instanceof ASMPopStack || 
				ins instanceof ASMLdr || ins instanceof ASMStr ||
				ins instanceof ASMLdrStack || ins instanceof ASMStrStack) continue;
			
			for (REG reg : REG.values()) {
				if (reg.isSpecialReg()) continue;
				
				if (ASMOptimizer.overwritesReg(ins, reg)) {
					
					List<REG> read = new ArrayList();
					for (REG reg0 : REG.values()) {
						if (reg0.isSpecialReg()) continue;
						if (ASMOptimizer.readsReg(ins, reg0))
							read.add(reg0);
					}
					
					for (int k = i + 1; k < ins0.size(); k++) {
						ASMInstruction ins1 = ins0.get(k);
						
						if (ins1 instanceof ASMLabel || ins1 instanceof ASMBranch) break;
						
						if (ASMOptimizer.overwritesReg(ins1, REG.PC)) break;
						
						boolean overwrite = false;
						for (REG reg0 : read) 
							overwrite |= ASMOptimizer.overwritesReg(ins1, reg0);
						if (overwrite) break;
						
						if (ASMOptimizer.overwritesReg(ins1, reg)) {
							if (ins1.build().equals(ins.build())) {
								/* Same instruction, can remove */
								ins0.remove(k);
								k--;
								OPT_DONE();
							}
							else break;
						}
					}
					
					break;
				}
			}
		}
	}
	
	public void pushDownFunctionFrame(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			/* Found function push */
			if (ins0.get(i) instanceof ASMPushStack && ins0.get(i).optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				boolean DID_OPT = false;
				
				/* Get list of all regs that are pushed in the function frame */
				List<REG> clearedRegs = new ArrayList();
				for (RegOp op : push.operands) 
					clearedRegs.add(op.reg);
				
				/* TODO: Check if FP push can be ignored */
				if (clearedRegs.contains(REG.FP)) continue;
				
				int wroteR0 = 0;
				List<ASMInstruction> ins1 = ins0.stream().collect(Collectors.toList());
				
				List<ASMLabel> visited = new ArrayList();
				List<REG> regsReadByInitial = new ArrayList();
				List<REG> clearedRegOccurrence = clearedRegs.stream().collect(Collectors.toList());
				
				int currentPosition = i + 1;
				int insertPosition = i;
				
				while (currentPosition < ins0.size()) {
					ASMInstruction ins = ins0.get(currentPosition);
					
					boolean overwritesPushedReg = false;
					for (REG reg : clearedRegs) overwritesPushedReg |= ASMOptimizer.overwritesReg(ins, reg);
					
					boolean readsPushedReg = false;
					for (REG reg : clearedRegs) readsPushedReg |= ASMOptimizer.readsReg(ins, reg);
					
					/* Break if either R0, R1 or R2 is overwritten */
					boolean opRegsOverwrite = false;
					for (REG reg : regsReadByInitial) {
						if (reg == REG.R0 && ASMOptimizer.overwritesReg(ins, reg)) {
							wroteR0 = 1;
						}
						else opRegsOverwrite |= ASMOptimizer.overwritesReg(ins, reg);
					}
				
					if (opRegsOverwrite) break;
						
					if (!overwritesPushedReg) {
						/* If it does not overwrite one of the regs it is not part of the initial push */
						if (ins instanceof ASMPopStack) {
							ASMPopStack pop = (ASMPopStack) ins;
							
							/* Is function frame pop, replace with bx lr */
							if (pop.operands.get(pop.operands.size() - 1).reg == REG.PC) {
								ins0.set(currentPosition, new ASMBranch(BRANCH_TYPE.BX, new RegOp(REG.LR)));
								DID_OPT = true;
							}
						}
						else if (readsPushedReg || ins instanceof ASMPushStack ||
								 ins instanceof ASMLabel && ins.optFlags.contains(OPT_FLAG.LOOP_HEAD)) {
							break;
						}
						else {
							if (ins instanceof ASMLabel) {
								ASMLabel label = (ASMLabel) ins;
								if (label.optFlags.contains(OPT_FLAG.LOOP_HEAD)) break;
								else {
									if (visited.contains(label)) 
										visited.remove(label);
								}
							}
							else if (ins instanceof ASMBranch) {
								/* Stop at function call */
								ASMBranch branch = (ASMBranch) ins;
								if (branch.type == BRANCH_TYPE.BL) break;
								
								if (ins.optFlags.contains(OPT_FLAG.BRANCH_TO_EXIT)) {
									wroteR0 = 0;
									ins = new ASMBranch(BRANCH_TYPE.BX, ins.cond, new RegOp(REG.LR));
									DID_OPT = true;
								}
								else {
									if (branch.target instanceof LabelOp) {
										LabelOp op = (LabelOp) branch.target;
										if (!visited.contains(op.label)) 
											visited.add(op.label);
									}
									else break;
								}
							}
							else if (ASMOptimizer.overwritesReg(ins, REG.PC)) {
								break;
							}
							
							ins0.remove(currentPosition);
							ins0.add(insertPosition++, ins);
							OPT_DONE();
						}
					}
					else {
						/* 
						 * We have to keep track which regs are read by
						 * the initial instructions. If any of these regs
						 * is overwritten, we have to stop.
						 */
						for (int a = 0; a < 11; a++) {
							REG reg = REG.toReg(a);
							if (ASMOptimizer.readsReg(ins, REG.toReg(a)) && !regsReadByInitial.contains(reg)) {
								regsReadByInitial.add(reg);
							}
						}
						
						boolean abort = false;
						for (REG reg : clearedRegs) {
							if (ASMOptimizer.overwritesReg(ins, reg)) {
								if (clearedRegOccurrence.contains(reg)) {
									/* Allow first overwrite, made by initial */
									clearedRegOccurrence.remove(reg);
								}
								else abort = true;
							}
						}
						
						if (abort) break;
					}
					
					if (wroteR0 > 0) wroteR0++;
					currentPosition++;
				}
				
				if (!visited.isEmpty() || wroteR0 > 1 || !DID_OPT) {
					ins0.clear();
					ins0.addAll(ins1);
				}
				else OPT_DONE();
			}
		}
	}
	
	public void removeFPExchange(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i);
				
				ASMPushStack push = null;
				if (ins0.get(i - 1) instanceof ASMPushStack) push = (ASMPushStack) ins0.get(i - 1);
				
				/* Found FP/SP exchange */
				if (mov.target.reg == REG.FP && mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == REG.SP) {
					boolean clear = true;
					ASMMov mov0 = null;
					
					List<ASMInstruction> patch = new ArrayList();
					
					for (int a = i + 1; a < ins0.size(); a++) {
						if (ins0.get(a) instanceof ASMMov) {
							mov0 = (ASMMov) ins0.get(a);
							if (mov0.target.reg == REG.SP && mov0.op1 instanceof RegOp && ((RegOp) mov0.op1).reg == REG.FP) {
								break;
							}
						}
						else patch.add(ins0.get(a));
						
						if (ins0.get(a) instanceof ASMPushStack && ins0.get(a).optFlags.contains(OPT_FLAG.STRUCT_INIT)) {
							clear = false;
							break;
						}
						else if (ins0.get(a) instanceof ASMBranch && ((ASMBranch) ins0.get(a)).type == BRANCH_TYPE.BL) {
							clear = false;
							break;
						}
						else if (ins0.get(a) instanceof ASMSub) {
							ASMSub sub = (ASMSub) ins0.get(a);
							
							if (sub.target.reg == REG.SP && sub.op0.reg == REG.SP) {
								clear = false;
								break;
							}
						}
					}
					
					if (clear) {
						if (push == null || push.operands.size() == 1 && push.operands.get(0).reg == REG.FP) {
							
							/* 
							 * Check if the patch contains direct asm. If yes, the operation may be unsafe since the
							 * direct ASM could interfere with the FP/SP.
							 */
							boolean containsDirectASM = patch.stream().filter(x -> x instanceof ASMHardcode).count() > 0;
							if (!containsDirectASM) {
								if (push != null) {
									ins0.remove(push);
									ins0.remove(push.popCounterpart);
								}
								
								ins0.remove(mov);
								
								this.patchFramePointerAddressing(patch, 4);
								
								this.patchFPtoSP(patch);
								
								i = ins0.indexOf(mov0) + 1;
								ins0.remove(mov0);
								
								OPT_DONE();
							}
						}
					}
				}
			}
		}
	}
	
	public void implicitReturn(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				RegOp lr = null;
				for (RegOp r : push.operands) if (r.reg == REG.LR) {
					lr = r;
					break;
				}
				
				/* Only do opt for push operations with only lr left */
				if (push.operands.size() > 1) continue;
				
				if (push.optFlags.contains(OPT_FLAG.FUNC_CLEAN) && lr != null) {
					ASMPopStack pop = push.popCounterpart;
					
					ASMBranch branch = null;
					boolean clear = true;
					for (int a = i + 1; a < ins0.size(); a++) {
						if (ins0.get(a).equals(pop)) break;
						if (ins0.get(a) instanceof ASMBranch) {
							if (branch != null) {
								clear = false;
								break;
							}
							else {
								ASMBranch b = (ASMBranch) ins0.get(a);
								if (b.type == BRANCH_TYPE.BL) {
									branch = b;
								}
							}
						}
						else if (ins0.get(a) instanceof ASMMov && ((ASMMov) ins0.get(a)).target.reg == REG.PC) {
							clear = false;
						}
						else if (branch != null) {
							clear = false;
							break;
						}
					}
					
					if (clear && branch != null) {
						push.operands.remove(lr);
						for (RegOp r : pop.operands) {
							if (r.reg == REG.LR) {
								pop.operands.remove(r);
								break;
							}
						}
						
						i = ins0.indexOf(pop) + 1;
						
						if (push.operands.isEmpty()) {
							ins0.remove(push);
							ins0.remove(pop);
							i -= 2;
						}
						
						branch.type = BRANCH_TYPE.B;
						branch.optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
						
						OPT_DONE();
					}
				}
			}
		}
	}
	
	public void popReturnDirect(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) ins0.get(i);
				if (branch.type == BRANCH_TYPE.BX && !branch.optFlags.contains(OPT_FLAG.BX_SEMI_EXIT) && ins0.get(i - 1) instanceof ASMPopStack) {
					ASMPopStack pop = (ASMPopStack) ins0.get(i - 1);
					
					if (pop.operands.size() > 0 && pop.operands.get(pop.operands.size() - 1).reg == REG.LR) {
						pop.operands.set(pop.operands.size() - 1, new RegOp(REG.PC));
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	public void patchFramePointerAddressing(List<ASMInstruction> body, int sub) {
		for (ASMInstruction ins : body) {
			if (ins instanceof ASMStackOp) {
				ASMStackOp stackOp = (ASMStackOp) ins;
				
				if (stackOp.op0 != null && stackOp.op0.reg == REG.FP) {
					if (stackOp.op1 instanceof ImmOp && ((ImmOp) stackOp.op1).patchable != null) {
						PatchableImmOp op = ((ImmOp) stackOp.op1).patchable;
						
						/* Patch the offset for parameters because they are located under the pushed regs,
						 * dont patch local data since its located above the pushed regs.
						 */
						if (op.dir == PATCH_DIR.UP) {
							((ImmOp) stackOp.op1).value -= sub;
						}
					}
				}
			}
			else if (ins instanceof ASMBinaryData) {
				ASMBinaryData binary = (ASMBinaryData) ins;
				
				if (binary.op0 != null && binary.op0.reg == REG.FP) {
					if (binary.op1 instanceof ImmOp && ((ImmOp) binary.op1).patchable != null) {
						PatchableImmOp op = ((ImmOp) binary.op1).patchable;
						
						if (op.dir == PATCH_DIR.UP) {
							((ImmOp) binary.op1).value -= sub;
						}
					}
					else ((ImmOp) binary.op1).value -= sub;
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp mem = (ASMMemOp) ins;
				
				if (mem.op0 != null && mem.op0 instanceof RegOp && ((RegOp) mem.op0).reg == REG.FP) {
					if (mem.op1 instanceof ImmOp && ((ImmOp) mem.op1).patchable != null) {
						PatchableImmOp op = ((ImmOp) mem.op1).patchable;
						
						if (op.dir == PATCH_DIR.UP) {
							((ImmOp) mem.op1).value -= sub;
						}
					}
				}
			}
		}
	}
	
	private void removeFuncCleanStrict(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack && ins0.get(i).optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				boolean done = false;
				List<ASMInstruction> ins = new ArrayList();
				
				if (push.popCounterpart != null) {
					ASMPopStack pop = push.popCounterpart;
					
					int sub = 0;
					
					for (int k = i + 1; k < ins0.indexOf(push.popCounterpart) - 1; k++) ins.add(ins0.get(k));

					/* Check betweeen interval */
					for (int x = 0; x < push.operands.size(); x++) {
						RegOp reg = push.operands.get(x);
						
						/* Only for regular registers */
						if (reg.reg.toInt() < 10) {
							
							/* Check if register was strictly not used */
							boolean regInteraction = false;
							for (int k = i + 1; k < ins0.indexOf(push.popCounterpart) - 1; k++) 
								regInteraction |= readsReg(ins0.get(k), reg.reg) || overwritesReg(ins0.get(k), reg.reg);
							
							if (!regInteraction) {
								push.operands.remove(x);
								pop.operands.remove(x);
								
								sub += 4;
								
								x--;
								OPT_DONE();
								done = true;
							}
						}
					}
					
					if (push.operands.isEmpty()) {
						ins0.remove(push);
						ins0.remove(pop);
						OPT_DONE();
						
						if (i > 0) i--;
					}
					
					if (done) patchFramePointerAddressing(ins, sub);
				}
			}
		}
	}
	
	private void bxSubstitution(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) ins0.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp label = (LabelOp) branch.target;
				
					int x = -1;
					
					/* Search for targeted label, indexOf is buggy in this situation :( */
					for (int a = 0; a < ins0.size(); a++) {
						if (ins0.get(a) instanceof ASMLabel) {
							ASMLabel l = (ASMLabel) ins0.get(a);
							if (label.label != null && l.name.equals(label.label.name)) {
								x = a;
								break;
							}
						}
					}
					
					if (x != -1 && ins0.get(x + 1) instanceof ASMBranch) {
						ASMBranch b0 = (ASMBranch) ins0.get(x + 1);
					
						if (b0.type == BRANCH_TYPE.BX) {
							for (int a = i; a < x + 1; a++) {
								if (ins0.get(a) instanceof ASMBranch) {
									branch = (ASMBranch) ins0.get(a);
									if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp && ((LabelOp) branch.target).label.equals(label.label)) {
										branch.type = BRANCH_TYPE.BX;
										branch.optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
										branch.target = b0.target.clone();
										
										OPT_DONE();
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void popPcSubstitution(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) ins0.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp label = (LabelOp) branch.target;
				
					int x = -1;
					
					/* Search for targeted label, indexOf is buggy in this situation :( */
					for (int a = 0; a < ins0.size(); a++) {
						if (ins0.get(a) instanceof ASMLabel) {
							ASMLabel l = (ASMLabel) ins0.get(a);
							if (label.label != null && l.name.equals(label.label.name)) {
								x = a;
								break;
							}
						}
					}
					
					if (x != -1 && ins0.get(x + 1) instanceof ASMPopStack) {
						ASMPopStack pop = (ASMPopStack) ins0.get(x + 1);
					
						if (ASMMemBlock.checkInOrder(pop.operands) || !CompilerDriver.optimizeFileSize) {
							if (pop.operands.get(pop.operands.size() - 1).reg == REG.PC) {
								for (int a = i; a < x + 1; a++) {
									if (ins0.get(a) instanceof ASMBranch) {
										branch = (ASMBranch) ins0.get(a);
										if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp && ((LabelOp) branch.target).label.equals(label.label)) {
											ins0.set(a, pop.clone());
											ins0.get(a).cond = branch.cond;
											OPT_DONE();
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void multPrecalc(List<ASMInstruction> ins0) {
		for (int i = 2; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMult) {
				ASMMult mul = (ASMMult) ins0.get(i);
				
				REG mReg0 = mul.op0.reg;
				REG mReg1 = mul.op1.reg;
				
				ImmOp op0 = null;
				ImmOp op1 = null;
				
				boolean last = false;
				
				if (ins0.get(i - 2) instanceof ASMMov) {
					ASMMov mov = (ASMMov) ins0.get(i - 2);
					if (mov.target.reg.toInt() < 3 && mov.op1 instanceof ImmOp) {
						if (mov.target.reg == mReg0) op0 = (ImmOp) mov.op1;
						if (mov.target.reg == mReg1) op1 = (ImmOp) mov.op1;
					}
				}
				
				if (ins0.get(i - 1) instanceof ASMMov) {
					ASMMov mov = (ASMMov) ins0.get(i - 1);
					if (mov.target.reg.toInt() < 3 && mov.op1 instanceof ImmOp) {
						if (mov.target.reg == mReg0) op0 = (ImmOp) mov.op1;
						if (mov.target.reg == mReg1) {
							op1 = (ImmOp) mov.op1;
							last = true;
						}
					}
				}
				
				if (op0 != null && op1 != null) {
					int r = op0.value * op1.value;
					
					/* Can only move a value 0 <= r <= 255 */
					if (r <= 255) {
						ins0.set(i, new ASMMov(new RegOp(mul.target.reg), new ImmOp(r)));
						
						/* Remove two movs */
						ins0.remove(i - 2);
						ins0.remove(i - 2);
						
						OPT_DONE();
					}
				}
				else if (op1 != null) {
					/* Convert to LSL if last operand is given and is a power of 2 */
					if (Math.pow(2, (int) (Math.log(op1.value) / Math.log(2))) == op1.value && last) {
						int log = (int)(Math.log(op1.value) / Math.log(2)); 
						ins0.set(i, new ASMLsl(mul.target, mul.op0, new ImmOp(log)));
						ins0.remove(i - 1);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void mulToMlaMerge(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMult && ins0.get(i).cond == null) {
				ASMMult mul = (ASMMult) ins0.get(i - 1);
				
				if (ins0.get(i) instanceof ASMAdd && ins0.get(i).cond == null) {
					ASMAdd add = (ASMAdd) ins0.get(i);
					
					if (mul.target.reg == add.op0.reg && add.op1 instanceof RegOp) {
						ASMMla mla = new ASMMla(add.target, mul.op0, mul.op1, (RegOp) add.op1);
						ins0.set(i - 1, mla);
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void removeIndirectMemOpAddressing(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMemOp) {
				ASMMemOp ldr = (ASMMemOp) ins0.get(i);
				
				if (ldr.op1 != null || !(ldr.op0 instanceof RegOp)) continue;
				
				REG dataTarget = null;
				RegOp op0 = null;
				Operand op1 = null;
				
				Shift shift = null;
				
				boolean negate = false;
				
				if (ins0.get(i - 1) instanceof ASMAdd) {
					ASMAdd add = (ASMAdd) ins0.get(i - 1);
					
					dataTarget = add.target.reg;
					op0 = add.op0.clone();
					op1 = add.op1.clone();
					
					shift = add.shift;
				}
				else if (ins0.get(i - 1) instanceof ASMSub) {
					ASMSub sub = (ASMSub) ins0.get(i - 1);
					
					dataTarget = sub.target.reg;
					op0 = sub.op0.clone();
					
					if (sub.op1 instanceof RegOp) {
						op1 = sub.op1.clone();
						negate = true;
					}
					else if (sub.op1 instanceof ImmOp) {
						ImmOp imm = (ImmOp) sub.op1;
						op1 = new ImmOp(-imm.value);
					}
					else continue;
				}
				else if (ins0.get(i - 1) instanceof ASMLsl) {
					/* 
					 * Will mostly target dereferencing operations. 
					 * Assumes that R10 is set to 0 at any given time. In most of the cases, this is given.
					 * In some special cases, like when calling the stack copy routine or the lambda head of a function,
					 * R10 is set temporary to the pc + 8. R10 is reset to 0 afterwards.
					 */
					ASMLsl lsl = (ASMLsl) ins0.get(i - 1);
					
					dataTarget = lsl.target.reg;
					op0 = new RegOp(REG.R10);
					
					op1 = lsl.op0.clone();
					shift = new Shift(SHIFT.LSL, lsl.op1.clone());
				}
				else continue;
				
				REG ldrTarget = ((RegOp) ldr.op0).reg;
					
				if (dataTarget != ldrTarget) continue;
				
				boolean clear = true;
				
				for (int a = i + 1; a < ins0.size(); a++) {
					if (readsReg(ins0.get(a), dataTarget)) {
						clear = false;
						break;
					}
					if (overwritesReg(ins0.get(a), dataTarget) && 
						!readsReg(ins0.get(a), dataTarget)) {
						break;
					}
					
					/* 
					 * In most cases, after a branch the sub instruction should
					 * not be relevant anymore, was only part of addressing.
					 * Still can be a potential problem causer, may need a stricter
					 * filter.
					 */
					if (ins0.get(a) instanceof ASMBranch) break;
				}
				
				/* Ldr itself will overwrite register */
				clear |= ldr.target.reg == dataTarget;
				
				if (clear) {
					if (shift == null || op1 != null) {
						/* Substitute */
						ldr.op0 = op0;
						ldr.op1 = op1;
						
						if (ldr.op1 instanceof RegOp)
							((RegOp) op1).shift = shift;
					}
					else {
						/* Special treatment for shifts */
						
						/* Is always 0 */
						ldr.op0 = new RegOp(REG.R10);
						
						ldr.op1 = op0;
						op0.shift = shift;
					}
					
					if (negate) ldr.subFromBase = true;
					
					ins0.remove(i - 1);
					i--;
					OPT_DONE();
				}
			}
		}
	}
	
	private void removeUnusedRegistersStrict(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMLabel && ((ASMLabel) ins0.get(i)).isFunctionLabel) {
				List<REG> probed = new ArrayList();
				
				for (int k = i; k < ins0.size(); k++) {
					if (ins0.get(k) instanceof ASMMov) {
						ASMMov mov = (ASMMov) ins0.get(k);
						REG reg = mov.target.reg;
						
						/*
						 *  Only probe first occurrence of assignment to register after 
						 *  function start. If mov instruction would not be first, but last
						 *  to assign to register, algorithm would delete the instruction
						 *  destroying the data flow.
						 */
						if (probed.contains(reg)) continue;
						else probed.add(reg);
						
						if (reg.toInt() > 2 && reg.toInt() < 10) {
							boolean used = false;
							for (int a = k + 1; a < ins0.size(); a++) {
								if (ins0.get(a) instanceof ASMLabel && ((ASMLabel) ins0.get(a)).isFunctionLabel) {
									break;
								}
								else {
									used |= readsReg(ins0.get(a), reg);
									
									/*
									 * A branch, for example from a for-loop, that branches
									 * back to the start of the loop makes this optimization
									 * impossible.
									 */
									if (ins0.get(a) instanceof ASMBranch) {
										ASMBranch branch = (ASMBranch) ins0.get(a);
										if (branch.optFlags.contains(OPT_FLAG.LOOP_BRANCH)) {
											used = true;
											break;
										}
									}
								}
							}
							
							if (!used) {
								ins0.remove(k);
								i--;
								OPT_DONE();
							}
						}
					}
				}
			}
		}
	}
	
	private void removeUnusedAssignment(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			REG reg = null;
			
			if (ins0.get(i) instanceof ASMBinaryData) {
				ASMBinaryData d = (ASMBinaryData) ins0.get(i);
				if (d.isUpdatingCondField() || d.cond != null) continue;
				reg = d.target.reg;
			}
			else if (ins0.get(i) instanceof ASMLdr) {
				ASMLdr d = (ASMLdr) ins0.get(i);
				if (d.cond != null) continue;
				reg = d.target.reg;
			}
			else if (ins0.get(i) instanceof ASMLdrStack) {
				ASMLdrStack d = (ASMLdrStack) ins0.get(i);
				if (d.cond != null) continue;
				reg = d.target.reg;
			}
			
			if (reg == null) continue;
			
			if (reg.toInt() < 3) {
				for (int a = i + 1; a < ins0.size(); a++) {
					ASMInstruction ins = ins0.get(a);
					
					if (readsReg(ins, reg) || ins instanceof ASMBranch || overwritesReg(ins, REG.PC)) {
						break;
					}
					else if (overwritesReg(ins, reg) && !readsReg(ins, reg)) {
						ins0.remove(i);
						i--;
						OPT_DONE();
						break;
					}
				}
			}
			else if (reg.toInt() < 10) {
				for (int a = i + 1; a < ins0.size(); a++) {
					ASMInstruction ins = ins0.get(a);
					
					if (readsReg(ins, reg) || ins.optFlags.contains(OPT_FLAG.LOOP_BRANCH)) {
						break;
					}
					else if (ins instanceof ASMLabel && ((ASMLabel) ins).isFunctionLabel || a == ins0.size() - 1) {
						ins0.remove(i);
						i--;
						OPT_DONE();
						break;
					}
				}
			}
		}
	}
	
	private void pushPopRelocation(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				REG reg = push.operands.get(0).reg;
				if (reg.isSpecialReg()) continue;
				
				if (push.operands.size() == 1 && !push.optFlags.contains(OPT_FLAG.FUNC_CLEAN) && !push.optFlags.contains(OPT_FLAG.STRUCT_INIT)) {
					
					ins0.remove(i);
					
					boolean afterFPExchange = false;
					int line = i;
					while (true) {
						if (overwritesReg(ins0.get(line), reg) || 
								ins0.get(line) instanceof ASMBranch || ins0.get(line) instanceof ASMLabel || 
								ins0.get(line) instanceof ASMStackOp || ins0.get(line) instanceof ASMPushStack ||
								ins0.get(line) instanceof ASMLdr || ins0.get(line) instanceof ASMPopStack ||
								ins0.get(line) instanceof ASMPushStack ||
								ASMOptimizer.overwritesReg(ins0.get(line), REG.PC)) {
							break;
						}
						else if (readsReg(ins0.get(line), REG.SP)) break;
						else {
							if (ins0.get(line) instanceof ASMMov) {
								ASMMov mov = (ASMMov) ins0.get(line);
								if (mov.target.reg == REG.SP && mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == REG.FP) {
									afterFPExchange = true;
								}
							}
							line++;
						}
					}
					
					if (line != i || afterFPExchange) OPT_DONE();
					if (!afterFPExchange) ins0.add(line, push);
				}
			}
		}
		
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) ins0.get(i);
				
				REG reg = pop.operands.get(0).reg;
				if (reg.isSpecialReg()) continue;
				
				if (pop.operands.size() == 1 && !pop.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
					ins0.remove(i);
					
					int line = i - 1;
					while (true) {
						if (readsReg(ins0.get(line), reg) || overwritesReg(ins0.get(line), reg) ||
								ins0.get(line) instanceof ASMBranch || ins0.get(line) instanceof ASMLabel || 
								ins0.get(line) instanceof ASMStackOp || 
								ins0.get(line) instanceof ASMStr || ins0.get(line) instanceof ASMPushStack ||
								ins0.get(line) instanceof ASMPopStack ||
								ASMOptimizer.overwritesReg(ins0.get(line), REG.PC)) {
							break;
						}
						
						line--;
					}
					
					if (line != i - 1) OPT_DONE();
					ins0.add(line + 1, pop);
					
					i = line + 2;
				}
			}
		}
	}
	
	private void defragmentAdditions(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMAdd && ins0.get(i - 1) instanceof ASMAdd) {
				ASMAdd add0 = (ASMAdd) ins0.get(i - 1);
				ASMAdd add1 = (ASMAdd) ins0.get(i);
				if (add0.target.reg == add1.target.reg && add0.target.reg == add1.op0.reg && 
						add0.op1 instanceof ImmOp && add1.op1 instanceof ImmOp) {
					ImmOp op0 = (ImmOp) add0.op1;
					ImmOp op1 = (ImmOp) add1.op1;
					
					op0.value += op1.value;
					ins0.remove(i);
					i--;
					OPT_DONE();
				}
			}
		}
		
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMAdd && ins0.get(i - 1) instanceof ASMAdd) {
				ASMAdd add0 = (ASMAdd) ins0.get(i - 1);
				ASMAdd add1 = (ASMAdd) ins0.get(i);
				
				if (add0.target.reg == add1.op0.reg && add0.op1 instanceof ImmOp && add1.op1 instanceof ImmOp && add0.target.reg.toInt() < 3) {
					ImmOp op0 = (ImmOp) add0.op1;
					ImmOp op1 = (ImmOp) add1.op1;
					
					op1.value = op0.value + op1.value;
					add1.op0.reg = add0.op0.reg;
					
					op0.value += op1.value;
					ins0.remove(i - 1);
					i--;
					OPT_DONE();
				}
			}
		}
	}
	
	private void defragmentPush(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack && ins0.get(i - 1) instanceof ASMPushStack) {
				ASMPushStack push0 = (ASMPushStack) ins0.get(i - 1);
				ASMPushStack push1 = (ASMPushStack) ins0.get(i);
				
				if (push0.operands.stream().filter(x -> x.reg.toInt() >= 10).count() > 0) continue;
				if (push1.operands.stream().filter(x -> x.reg.toInt() >= 10).count() > 0) continue;
				
				List<REG> op0 = push0.operands.stream().map(x -> x.reg).collect(Collectors.toList());
				List<REG> op1 = push1.operands.stream().map(x -> x.reg).collect(Collectors.toList());
				
				/* Make sure no REGs of the push1 are contained in the REGs of push0 */
				boolean contains = false;
				for (REG reg : op1) 
					if (op0.contains(reg)) 
						contains = true;
				if (contains) continue;
				
				for (int a = 0; a < push1.operands.size(); a++) {
					RegOp reg = push1.operands.get(a);
					if (push0.operands.stream().filter(x -> x.reg == reg.reg).count() == 0) {
						push0.operands.add(0, reg);
						push1.operands.remove(a--);
						OPT_DONE();
					}
				}
				
				if (push1.operands.isEmpty()) {
					ins0.remove(i--);
					OPT_DONE();
				}
			}
		}
	}
	
	private void shiftBy0IsMov(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			if (ins instanceof ASMLsl) {
				ASMLsl lsl = (ASMLsl) ins;
				if (lsl.op1 != null && lsl.op1 instanceof ImmOp) {
					ImmOp imm = (ImmOp) lsl.op1;
					if (imm.value == 0) {
						ins0.set(i, new ASMMov(lsl.target, lsl.op0, lsl.cond));
						OPT_DONE();
					}
				}
			}
			else if (ins instanceof ASMLsr) {
				ASMLsr lsr = (ASMLsr) ins;
				if (lsr.op1 != null && lsr.op1 instanceof ImmOp) {
					ImmOp imm = (ImmOp) lsr.op1;
					if (imm.value == 0) {
						ins0.set(i, new ASMMov(lsr.target, lsr.op0, lsr.cond));
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void defragmentDeltas(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMAdd && ins0.get(i - 1) instanceof ASMSub) {
				ASMSub sub = (ASMSub) ins0.get(i - 1);
				ASMAdd add = (ASMAdd) ins0.get(i);
				
				if (sub.target.reg == add.target.reg && add.target.reg == add.op0.reg && 
						sub.op1 instanceof ImmOp && add.op1 instanceof ImmOp) {
					ImmOp subOp = (ImmOp) sub.op1;
					ImmOp addOp = (ImmOp) add.op1;
					
					if (subOp.value >= addOp.value) {
						subOp.value -= addOp.value;
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void additionCommutative(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMAdd && ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				ASMAdd add = (ASMAdd) ins0.get(i);
				
				if (mov.target.reg == add.op0.reg && mov.op1 instanceof ImmOp && add.op1 instanceof RegOp && add.shift == null) {
					RegOp op1 = (RegOp) add.op1;
					add.op1 = mov.op1;
					add.op0 = op1;
					
					ins0.remove(i - 1);
					i--;
					OPT_DONE();
				}
			}
		}
		
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMAdd && ins0.get(i - 1) instanceof ASMLsl) {
				ASMLsl lsl = (ASMLsl) ins0.get(i - 1);
				ASMAdd add = (ASMAdd) ins0.get(i);
				
				if (lsl.target.reg == add.op0.reg && lsl.op1 instanceof ImmOp && add.op1 instanceof RegOp && add.shift == null) {
					RegOp op1 = (RegOp) add.op1;
					add.op1 = lsl.op0;
					
					add.shift = new Shift(SHIFT.LSL, lsl.op1);
					
					add.op0 = op1;
					
					ins0.remove(i - 1);
					i--;
					OPT_DONE();
				}
			}
		}
	}

	private void subtractionSemiCommutative(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMSub && ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				ASMSub sub = (ASMSub) ins0.get(i);
				
				if (mov.target.reg == sub.op0.reg && mov.op1 instanceof ImmOp && sub.op1 instanceof RegOp) {
					RegOp op1 = (RegOp) sub.op1;
					
					ins0.set(i, new ASMRsb(sub.target, op1, mov.op1));
					
					ins0.remove(i - 1);
					i--;
					OPT_DONE();
				}
			}
		}
	}
	
	private void removeZeroInstruction(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMAdd) {
				ASMAdd add = (ASMAdd) ins0.get(i);
				if (add.target.reg == add.op0.reg && add.op1 instanceof ImmOp && !add.isUpdatingCondField()) {
					ImmOp imm = (ImmOp) add.op1;
					if (imm.value == 0) {
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
			else if (ins0.get(i) instanceof ASMSub) {
				ASMSub sub = (ASMSub) ins0.get(i);
				if (sub.target.reg == sub.op0.reg && sub.op1 instanceof ImmOp && !sub.isUpdatingCondField()) {
					ImmOp imm = (ImmOp) sub.op1;
					if (imm.value == 0) {
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void removeUnnessesaryPushPop(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				boolean remove = false;
				
				if (push.operands.size() == 1) {
					REG reg = ((RegOp) push.operands.get(0)).reg;
					for (int a = i; a < ins0.size(); a++) {
						ASMInstruction ins = ins0.get(a);
						
						if (ins instanceof ASMPopStack) {
							ASMPopStack pop = (ASMPopStack) ins;
							
							if (pop.operands.size() == 1 && !pop.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
								RegOp op0 = (RegOp) pop.operands.get(0);
								if (op0.reg == reg) {
									ins0.remove(a);
									remove = true;
									break;
								}
							}
						}
						
						if (overwritesReg(ins, reg) || ins instanceof ASMBranch || ins instanceof ASMLabel) {
							break;
						}
					}
				}
				
				if (remove) {
					ins0.remove(i);
					i--;
				}
			}
		}
	}
	
	private void removeIndirectPushPopAssign(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins0.get(i);
				
				if (push.operands.size() == 1) {
					REG pushReg = push.operands.get(0).reg;
					
					/* Search for pop Counterpart */
					boolean found = false;
					ASMPopStack pop = null;
					int end = 0;
					for (int a = i + 1; a < ins0.size(); a++) {
						ASMInstruction ins = ins0.get(a);
						
						if (ins instanceof ASMBranch || ins instanceof ASMLabel || ins instanceof ASMPushStack) {
							break;
						}
						else if (ins instanceof ASMPopStack) {
							ASMPopStack pop0 = (ASMPopStack) ins;
							if (pop0.operands.size() == 1 && !pop0.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
								found = true;
								pop = pop0;
								end = a;
								break;
							}
						}
					}
					
					if (found) {
						boolean replace = true;
						REG newReg = pop.operands.get(0).reg;
						
						/* Check if register if newReg is overwritten in the span between the push pop */
						for (int a = i + 1; a < end; a++) {
							/* Old register is overwritten, value from mov would be shadowed */
							if (overwritesReg(ins0.get(a), push.operands.get(0).reg)) {
								replace = false;
								break;
							}
						}
						
						if (replace) {
							ins0.set(ins0.indexOf(pop), new ASMMov(new RegOp(newReg), new RegOp(pushReg)));
							ins0.remove(push);
							
							OPT_DONE();
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeExpressionIndirectTargeting(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					if (reg.isOperandReg()) {
						
						boolean replace = true;
						
						if (ins0.get(i + 1) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) ins0.get(i + 1);
							if (cmp.op0.reg == reg) replace = false;
						}
						
						boolean patchUp = false;
						if (ins0.get(i - 1) instanceof ASMBinaryData && !(ins0.get(i - 1) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) ins0.get(i - 1);
							
							if (data instanceof ASMVCvt) {
								patchUp = false;
								break;
							}
							
							if (data.target.reg == reg && replace) {
								data.target = mov.target;
								OPT_DONE();
								patchUp = true;
							}
						}
						else if (ins0.get(i - 1) instanceof ASMMult) {
							ASMMult mul = (ASMMult) ins0.get(i - 1);
							if (mul.target.reg == reg && replace) {
								mul.target = mov.target;
								OPT_DONE();
								patchUp = true;
							}
						}
						
						if (patchUp) ins0.remove(i);
					}
				}
			}
		}
	}
	
	private void removeLdrIndirectTargeting(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					
					/* Only perform action if target is a operand register. */
					if (reg.toInt() > 2) continue;
					
					if (ins0.get(i - 1) instanceof ASMLdrStack) {
						ASMLdrStack ldr = (ASMLdrStack) ins0.get(i - 1);
						
						if (ldr.target.reg == reg) {
							ldr.target.reg = mov.target.reg;
							OPT_DONE();
							ins0.remove(i);
							i--;
						}
					}
					else if (ins0.get(i - 1) instanceof ASMLdrLabel) {
						ASMLdrLabel ldr = (ASMLdrLabel) ins0.get(i - 1);
						
						if (ldr.target.reg == reg) {
							if (mov instanceof ASMVMov) {
								ASMVLdrLabel ldrLabel = new ASMVLdrLabel(ldr.target, (LabelOp) ldr.op0, ldr.dec);
								ldrLabel.prefix = ldr.prefix;
								
								ins0.set(i - 1, ldrLabel);
							}
							
							ldr.target.reg = mov.target.reg;
							OPT_DONE();
							ins0.remove(i);
							i--;
						}
					}
					else if (ins0.get(i - 1) instanceof ASMLdr) {
						ASMLdr ldr = (ASMLdr) ins0.get(i - 1);
						
						if (ldr.target.reg == reg) {
							if (mov instanceof ASMVMov) 
								ins0.set(i - 1, new ASMVLdr(ldr.target, ldr.op0, ldr.op1));
							
							ldr.target.reg = mov.target.reg;
							OPT_DONE();
							ins0.remove(i);
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeStrOperandIndirectTargeting(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					
					if (ins0.get(i) instanceof ASMStrStack) {
						ASMStrStack str = (ASMStrStack) ins0.get(i);
						
						if (str.target.reg == mov.target.reg) {
							str.target.reg = reg;
							OPT_DONE();
							ins0.remove(i - 1);
							i--;
						}
					}
					else if (ins0.get(i) instanceof ASMStr) {
						ASMStr str = (ASMStr) ins0.get(i);
						
						if (str.target.reg == mov.target.reg) {
							str.target.reg = reg;
							OPT_DONE();
							ins0.remove(i - 1);
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeOperandIndirectTargeting(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				/* For all operand regs */
				for (REG a : REG.values()) {
					if (!a.isOperandReg()) continue;
					if (mov.target.reg == a && mov.op1 instanceof RegOp) {
						if (ins0.get(i) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) ins0.get(i);
							if (cmp.op0 != null && cmp.op0.reg == a) {
								/* Replace */
								cmp.op0 = (RegOp) mov.op1;
								OPT_DONE();
								
								ins0.remove(i - 1);
								i--;
							}
							else if (cmp.op1 != null && cmp.op1 instanceof RegOp && ((RegOp) cmp.op1).reg == a) {
								/* Replace */
								cmp.op1 = (RegOp) mov.op1;
								OPT_DONE();
								
								ins0.remove(i - 1);
								i--;
							}
						}
					}
				}
			}
		}
		
		/**
		 * mov r0, r3
		 * str r0, [rx]
		 * ->
		 * str r3, [rx]
		 */
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMStr && ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				ASMStr str = (ASMStr) ins0.get(i);
				
				if (str.target.reg == mov.target.reg && mov.op1 instanceof RegOp) {
					boolean clear = true;
					for (int a = i + 1; a < ins0.size(); a++) {
						if (readsReg(ins0.get(a), mov.target.reg)) {
							clear = false;
							break;
						}
						
						if (overwritesReg(ins0.get(a), mov.target.reg))
							break;
					}
					
					if (clear) {
						ins0.remove(i - 1);
						str.target = (RegOp) mov.op1;
						i--;
						OPT_DONE();
					}
				}
			}
		}
		
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				for (int a = 0; a < 10; a++) {
					if (mov.target.reg == REG.toReg(a) && mov.op1 instanceof RegOp) {
						RegOp target = (RegOp) mov.op1;
						
						/* Writeback flag is set, cannot substitute, since mov copies operand */
						if (ins0.get(i).optFlags.contains(OPT_FLAG.WRITEBACK)) {
							break;
						}
						
						if (ins0.get(i) instanceof ASMBinaryData && !(ins0.get(i) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) ins0.get(i);
							
							boolean remove = false;
							if (data.op0 != null && data.op0.reg == REG.toReg(a)) {
								/* Replace */
								data.op0 = target;
								OPT_DONE();
								remove = true;
							}
							
							if (data.op1 != null && data.op1 instanceof RegOp && ((RegOp) data.op1).reg == REG.toReg(a)) {
								/* Replace */
								data.op1 = target;
								OPT_DONE();
								remove = true;
							}
							
							if (remove && a < 3) {
								ins0.remove(i - 1);
								i--;
							}
						}
						else if (ins0.get(i) instanceof ASMMult) {
							ASMMult mul = (ASMMult) ins0.get(i);
							boolean remove = false;
							if (mul.op0 != null && mul.op0.reg == REG.toReg(a)) {
								/* Replace */
								mul.op0 = target;
								OPT_DONE();
								remove = true;
							}
							
							if (mul.op1 != null && mul.op1 instanceof RegOp && ((RegOp) mul.op1).reg == REG.toReg(a)) {
								/* Replace */
								mul.op1 = target;
								OPT_DONE();
								remove = true;
							}
							
							if (remove && a < 3) {
								ins0.remove(i - 1);
								i--;
							}
						}
						else if (i + 1 < ins0.size() && ins0.get(i + 1) instanceof ASMMult) {
							
							ASMInstruction ins1 = ins0.get(i);
							if (!(ins1 instanceof ASMBranch || ins1 instanceof ASMLabel || 
								 ins1 instanceof ASMMemOp || ins1 instanceof ASMStackOp || 
								 ins1 instanceof ASMPushStack || ins1 instanceof ASMPopStack)) {
								
								ASMMult mul = (ASMMult) ins0.get(i + 1);
								boolean remove = false;
								
								if (overwritesReg(ins1, target.reg)) continue;
								
								if (mul.op0 != null && mul.op0.reg == REG.toReg(a)) {
									/* Replace */
									mul.op0 = target;
									OPT_DONE();
									remove = true;
								}
								
								if (mul.op1 != null && mul.op1 instanceof RegOp && mul.op1.reg == REG.toReg(a)) {
									/* Replace */
									mul.op1 = target;
									OPT_DONE();
									remove = true;
								}
								
								if (remove && a < 3 && !mov.optFlags.contains(OPT_FLAG.WRITEBACK)) {
									ins0.remove(i - 1);
									i--;
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void removeImplicitPushPopTargeting(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) ins0.get(i - 1);
				
				if (mov.target.reg == REG.R0 && mov.op1 instanceof RegOp) {
					if (ins0.get(i) instanceof ASMPushStack) {
						ASMPushStack push = (ASMPushStack) ins0.get(i);
						
						if (push.operands.size() == 1 && push.operands.get(0).reg == mov.target.reg) {
							push.operands.get(0).reg = ((RegOp) mov.op1).reg;
							ins0.remove(i - 1);
							i--;
							OPT_DONE();
						}
					}
				}
			}
		}
	}
	
	private void removeBranchesBeforeLabelToLabel(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMBranch && ins0.get(i) instanceof ASMLabel) {
				ASMBranch branch = (ASMBranch) ins0.get(i - 1);
				ASMLabel label = (ASMLabel) ins0.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp op = (LabelOp) branch.target;
					
					if (op.label.name.equals(label.name)) {
						ins0.remove(i - 1);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void removeDoubleCrossing(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i - 1) instanceof ASMMov && ins0.get(i) instanceof ASMMov) {
				ASMMov move0 = (ASMMov) ins0.get(i - 1);
				ASMMov move1 = (ASMMov) ins0.get(i);
				if (move0.op1 instanceof RegOp && move1.op1 instanceof RegOp) {
					RegOp op0 = (RegOp) move0.op1;
					RegOp op1 = (RegOp) move1.op1;
					if (op0.reg == REG.FP || op1.reg == REG.FP) continue;
					
					if (op0.reg == move1.target.reg && move0.target.reg == op1.reg) {
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void removeMovId(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMMov) {
				/* Remove identity mov */
				ASMMov mov = (ASMMov) ins0.get(i);
				if (mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == mov.target.reg) {
					ins0.remove(i);
					i--;
					OPT_DONE();
				}
			}
		}
	}
	
	private void constantOperandPropagation(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMMov) {
				ASMMov move = (ASMMov) ins0.get(i);
				
				if (move.cond != null) continue;
				
				if (move.op1 instanceof ImmOp) {
					int val = ((ImmOp) move.op1).value;
					REG target = move.target.reg;
					
					/* Set to false if reg is read */
					boolean clear = true;
					
					boolean hardClear = false;
					
					/* Break is reg is overwritten */
					
					for (int a = i + 1; a < ins0.size(); a++) {
						if (ins0.get(a) instanceof ASMBranch) {
							ASMBranch branch = (ASMBranch) ins0.get(a);
							if (branch.cond != null || branch.type != BRANCH_TYPE.B) {
								clear = false;
								break;
							}
						}
						
						if (ins0.get(a) instanceof ASMLabel ||
							ins0.get(a) instanceof ASMMov && ((ASMMov) ins0.get(a)).target.reg == REG.PC) {
							clear = false;
							break;
						}
						
						if (ins0.get(a).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
						
						if (ins0.get(a) instanceof ASMComment) {
							continue;
						}
						/* Substitute immediate value into move */
						else if (ins0.get(a) instanceof ASMMov) {
							ASMMov move0 = (ASMMov) ins0.get(a);
							
							if (move0.target.reg == target) {
								if (move0.cond != null) clear = false;
								break;
							}
							
							if (move0.op1 instanceof RegOp && ((RegOp) move0.op1).reg == target) {
								move0.op1 = new ImmOp(val);
								OPT_DONE();
							}
						}
						else if (ins0.get(a) instanceof ASMMvn) {
							ASMMvn move0 = (ASMMvn) ins0.get(a);
							
							if (move0.target.reg == target) {
								if (move0.cond != null) clear = false;
								break;
							}
							
							if (move0.op1 instanceof RegOp && ((RegOp) move0.op1).reg == target) {
								move0.op1 = new ImmOp(val);
								OPT_DONE();
							}
						}
						else if (ins0.get(a) instanceof ASMBinaryData) {
							ASMBinaryData dataP = (ASMBinaryData) ins0.get(a);
							
							if (dataP.optFlags.contains(OPT_FLAG.WRITEBACK)) {
								clear = false;
								break;
							}
							
							if (dataP.op1 instanceof RegOp && ((RegOp) dataP.op1).reg == target) {
								dataP.op1 = new ImmOp(val);
								OPT_DONE();
								
								if (!move.optFlags.contains(OPT_FLAG.WRITEBACK)) hardClear = true;
							}
							
							if (dataP.op0 instanceof RegOp && ((RegOp) dataP.op0).reg == target) {
								/* mov r1, #10; add r0, r1, #5 -> mov r0, #15 */
								if (dataP.op1 instanceof ImmOp) {
									int value = ((ImmOp) dataP.op1).value;
									ASMInstruction ins = null;
									
									int result = dataP.solver.solve(val, value);
									if (result >= 0) {
										ins = new ASMMov(dataP.target, new ImmOp(result));
									}
									else {
										/* Invert in twos complement */
										int inv = -(result + 1);
										ins = new ASMMvn(dataP.target, new ImmOp(inv));
									}
									
									ins0.set(a, ins);
									OPT_DONE();
									a -= 2;
								}
								else clear = false;
							}
							if (dataP.target.reg == target) {
								break;
							}
						}
						else if (ins0.get(a) instanceof ASMMult) {
							ASMMult mult = (ASMMult) ins0.get(a);
							
							if (mult.op0.reg == target || mult.op1.reg == target) {
								clear = false;
							}
							
							if (mult.target.reg == target) {
								break;
							}
						}
						else if (ins0.get(a) instanceof ASMMla) {
							ASMMla mla = (ASMMla) ins0.get(a);
							
							if (mla.op0.reg == target || mla.op1.reg == target || mla.op2.reg == target) {
								clear = false;
							}
							
							if (mla.target.reg == target) {
								break;
							}
						}
						else if (ins0.get(a) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) ins0.get(a);
							
							if (cmp.op1 instanceof RegOp && ((RegOp) cmp.op1).reg == target) {
								cmp.op1 = new ImmOp(val);
								OPT_DONE();
							}
							
							if (cmp.op0.reg == target) {
								clear = false;
							}
						}
						else if (ins0.get(a) instanceof ASMPushStack) {
							ASMPushStack p = (ASMPushStack) ins0.get(a);
							for (RegOp r : p.operands) {
								if (r.reg == target) {
									clear = false;
									break;
								}
							}
						}
						else if (ins0.get(a) instanceof ASMPopStack) {
							ASMPopStack p = (ASMPopStack) ins0.get(a);
							boolean end = false;
							for (RegOp r : p.operands) {
								if (r.reg == target) {
									end = true;
									break;
								}
							}
							
							if (end) break;
						}
						else if (ins0.get(a) instanceof ASMLdrStack) {
							ASMLdrStack p = (ASMLdrStack) ins0.get(a);
							if (p.target.reg == target) {
								break;
							}
							
							if (p.op0.reg == target) {
								clear = false;
							}
							
							if (p.op1 instanceof RegOp) {
								RegOp r = (RegOp) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOp(val);
									OPT_DONE();
								}
							}
						}
						else if (ins0.get(a) instanceof ASMLdr) {
							ASMLdr p = (ASMLdr) ins0.get(a);
							if (p.target.reg == target) {
								break;
							}
							
							if (p.op0 instanceof RegOp && ((RegOp) p.op0).reg == target) {
								clear = false;
							}
							
							if (p.op1 instanceof RegOp) {
								RegOp r = (RegOp) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOp(val);
									OPT_DONE();
								}
							}
						}
						else if (ins0.get(a) instanceof ASMStr) {
							ASMStr p = (ASMStr) ins0.get(a);
							if (p.target.reg == target) {
								clear = false;
								break;
							}
							
							if (p.op0 instanceof RegOp && ((RegOp) p.op0).reg == target) {
								clear = false;
							}
							
							if (p.op1 instanceof RegOp) {
								RegOp r = (RegOp) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOp(val);
									OPT_DONE();
								}
							}
						}
						else if (ins0.get(a) instanceof ASMStrStack) {
							ASMStrStack p = (ASMStrStack) ins0.get(a);
							if (p.target.reg == target) {
								clear = false;
								break;
							}
							
							if (p.op0.reg == target || (p.op1 instanceof RegOp && ((RegOp) p.op1).reg == target)) {
								clear = false;
							}
						}
						else if (ins0.get(a) instanceof ASMHardcode) {
							clear = false;
							break;
						}
						else if (ins0.get(a) instanceof ASMSeperator) {
							/* Do nothing */
						}
						else {
							clear = false;
							new Message("ASMOPT -> ConstOp propagation : Not available " + ins0.get(a).getClass().getName(), LogPoint.Type.WARN);
						}
						
					}
					
					if (clear || hardClear) {
						ins0.remove(i);
						i--;
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void clearUnusedLabels(List<ASMInstruction> ins0) {
		List<ASMLabel> usedLabels = new ArrayList();
		
		for (int i = 1; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			if (ins instanceof ASMLabel) {
				ASMLabel label = (ASMLabel) ins;
				/* Label is used by default if its a function header or a data label */
				if (label.isFunctionLabel || label instanceof ASMDataLabel || 
						label.name.equals("main_init") || label.name.equals("name") ||
							label.optFlags.contains(OPT_FLAG.LABEL_USED)) {
					
					usedLabels.add((ASMLabel) ins);
				}
			}
			if (ins instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) ins;
				if (b.target instanceof LabelOp && ((LabelOp) b.target).label != null) 
					usedLabels.add(((LabelOp) b.target).label);
			}
		}
		
		for (int i = 1; i < ins0.size(); i++) {
			ASMInstruction ins = ins0.get(i);
			if (ins instanceof ASMLabel) {
				boolean contained = false;
				
				for (ASMLabel label : usedLabels) 
					contained |= label.name.equals(((ASMLabel) ins).name);
				
				if (!contained) {
					ins0.remove(i);
					OPT_DONE();
					while (i < ins0.size() && !(ins instanceof ASMLabel)) {
						ins0.remove(i);
					}
					i--;
				}
			}
		}
	}
	
	private void clearInstructionsAfterBranch(List<ASMInstruction> ins0) {
		for (int i = 0; i < ins0.size(); i++) {
			if (ins0.get(i).optFlags.contains(OPT_FLAG.IS_PADDING)) continue;
			
			if (ins0.get(i) instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) ins0.get(i);
				if (b.cond == null && b.type != BRANCH_TYPE.BL && !b.optFlags.contains(OPT_FLAG.SYS_JMP)) {
					while (i < ins0.size() - 1 && !(ins0.get(i + 1) instanceof ASMSeperator)) {
						if (ins0.get(i + 1) instanceof ASMLabel && !(ins0.get(i + 1) instanceof ASMDataLabel)) break;
						if (ins0.get(i + 1) instanceof ASMComment || ins0.get(i + 1) instanceof ASMDataLabel) {
							i++;
							continue;
						}
						ins0.remove(i + 1);
						OPT_DONE();
					}
				}
			}
		}
	}
	
	private void removeDoubleLabels(List<ASMInstruction> ins0) {
		for (int i = 1; i < ins0.size(); i++) {
			if (ins0.get(i) instanceof ASMLabel && !(ins0.get(i) instanceof ASMDataLabel)) {
				ASMLabel l1 = (ASMLabel) ins0.get(i);
				
				if (ins0.get(i - 1) instanceof ASMLabel && !(ins0.get(i - 1) instanceof ASMDataLabel)) {
					ASMLabel l0 = (ASMLabel) ins0.get(i - 1);
					
					l1.name = l0.name;
					ins0.remove(i);
					i--;
					OPT_DONE();
				}
			}
		}
	}
	
} 
