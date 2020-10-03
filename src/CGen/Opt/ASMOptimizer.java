package CGen.Opt;

import java.util.ArrayList;
import java.util.List;

import Exc.SNIPS_EXC;
import Imm.ASM.ASMHardcode;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
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
import Imm.ASM.Util.Shift;
import Imm.ASM.Util.Shift.SHIFT;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.Message;

/**
 * This optimizer can simplify a sequence of asm instructions. By doing so
 * the optimizer will not change the behaviour of the program itself.<br>
 * The optimizer follows an internal logic of the Compiler, so this optimizer
 * will not work for any Assembly program, since it may violate the compiler internal
 * logic. This may lead to context changing optimizations.
 */
public class ASMOptimizer {

			/* --- FIELDS --- */
	/** Wether an optimization was done in this cycle and a new iteration should be launched. */
	boolean OPT_DONE = true;

	int iterations = 0, ins_pre;
	long opts = 0;
	
			/* --- METHODS --- */
	/** Optimize given body. */
	public void optimize(AsNBody body) {
		
		ins_pre = body.instructions.size();
		
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
			this.defragmentAdditions(body);
			
			/**
			 * sub r0, fp, #8
			 * add r0, r0, #4
			 * Replace with:
			 * sub r0, fp, #4
			 */
			this.defragmentDeltas(body);
			
			/**
			 * mov r0, #10
			 * add r0, r0, r2
			 * Replace with:
			 * add r0, r2, #10
			 */
			this.additionCommutative(body);
			
			/**
			 * mov r0, #10
			 * sub r0, r0, r2
			 * Replace with:
			 * rsb r0, r2, #10
			 */
			this.subtractionSemiCommutative(body);
			
			/**
			 * add r0, r1, r2
			 * mov r4, r0
			 * Replace with:
			 * add r4, r1, r2
			 */
			this.removeExpressionIndirectTargeting(body);
			
			/**
			 * ldr r0, [r1]
			 * mov r4, r0
			 * Replace with:
			 * add r4, [r1]
			 */
			this.removeLdrIndirectTargeting(body);
			
			/**
			 * mov r0, rx
			 * cmp rx, r1
			 * Replace with:
			 * cmp rx, r1
			 */
			this.removeOperandIndirectTargeting(body);
			
			/**
			 * Substitute immediate values after assignment
			 * to register into other instructions.
			 */
			this.constantOperandPropagation(body);
			
			/**
			 * b .L1
			 * ... <- Remove these Lines until Label or newline
			 */
			this.clearInstructionsAfterBranch(body);
			
			/**
			 * Removes non-referenced Labels
			 */
			this.clearUnusedLabels(body);
			
			/**
			 * mov r1, r0
			 * mov r0, r1 <-- Delete this line
			 */
			this.removeDoubleCrossing(body);
			
			/**
			 * .L0:
			 * .L1:
			 * Replace with:
			 * .L1   <- Rename all .L0 occurrenced to this label
			 */
			this.removeDoubleLabels(body);
			
			/**
			 * b .Lx <-- Remove this line
			 * .Lx: 
			 */
			this.removeBranchesBeforeLabelToLabel(body);
			
			/**
			 * push { r0 }
			 * ... <- Does not reassign r0
			 * pop { r0 }
			 */
			this.removeUnnessesaryPushPop(body);
			
			/**
			 * mov r0, rx
			 * push { r0 }
			 * Replace with:
			 * push { rx }
			 */
			this.removeImplicitPushPopTargeting(body);
			
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
			this.removeIndirectPushPopAssign(body);
			
			/**
			 * add r0, r0, #0 <- Remove this line
			 */
			this.removeZeroInstruction(body);
			
			/**
			 * instruction a <- Assigns to rx
			 * ... <- Do not reassign or read rx
			 * instruction b <- Assigns to rx
			 * 
			 * -> Remove instruction 
			 */
			this.removeUnusedAssignment(body);
			
			/**
			 * Removes mov rx, ... instructions, where rx = r3 - r9,
			 * and rx is not being used until a bx lr
			 */
			this.removeUnusedRegistersStrict(body);
			
			/**
			 * Attempt to collect immediate operands before a mult,
			 * and try to precalculate the mult based on the operands.
			 */
			this.multPrecalc(body);
			
			this.removeMovId(body);
			
			/**
			 * lsl r0, r1, #0
			 * Replace with:
			 * mov r0, r1
			 */
			this.shiftBy0IsMov(body);
			
			if (!OPT_DONE) {
				/**
				 * add r0, fp, #4
				 * ldr r0, [r0]
				 * Replace with:
				 * ldr r0, [fp, #4]
				 */
				this.removeIndirectMemOpAddressing(body);
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
				this.pushPopRelocation(body);
			}
			
			if (!OPT_DONE) {
				/**
				 * Checks through all function bodies and removes the pushed/popped regs 
				 * at the function start/end, which are strictly not used in the function
				 * body.
				 */
				this.removeFuncCleanStrict(body);
			}
			
			if (!OPT_DONE) {
				/**
				 * Checks if function body only contains one bl-Branch and if the branch is
				 * directley before the pop func clean. If yes, remove the lr push/pop and
				 * set the branch type to b. This means the branched function will return to
				 * the original caller.
				 */
				this.implicitReturn(body);
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
				this.removeFPExchange(body);
			}
			
			if (!OPT_DONE) {

				this.mulToMlaMerge(body);
				
			}
			
			if (!OPT_DONE) {
				
				this.bxSubstitution(body);
				
			}
			
			if (!OPT_DONE) {

				this.removeStrOperandIndirectTargeting(body);
				
			}
			
			if (!OPT_DONE) {
				
				this.deepRegPropagation(body);
				
			}
			
		}

		/**
		 * Check if a bx lr statement is directley after the func clean pop. If the last
		 * pop operand is a lr, substitute pc in the operand and remove the bx lr.
		 */
		this.popReturnDirect(body);
		
		/* Finishing touches, no iteration required */
		this.popPcSubstitution(body);
		this.clearUnusedLabels(body);
		
		/* Replace sub-structure loads with ldm */
		this.replaceR0R1R2LdrWithLdm(body);
		
		/* Replace large push/pop operations with ldm/stm */
		this.replacePushPopWithBlockMemory(body);
		
		/* Remove #0 operands from ldr and str instructions */
		this.removeZeroOperands(body);
		
		/* Filter duplicate empty lines */
		if (body.instructions.size() > 1) {
			for (int i = 1; i < body.instructions.size(); i++) {
				if (body.instructions.get(i - 1) instanceof ASMSeperator && body.instructions.get(i) instanceof ASMSeperator) {
					body.instructions.remove(i);
					i--;
				}
			}
		}
	}
	
	public void replaceR0R1R2LdrWithLdm(AsNBody body) {
		for (int i = 2; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 2) instanceof ASMLdr) {
				ASMLdr ldr0 = (ASMLdr) body.instructions.get(i - 2);
				
				if (ldr0.op0 instanceof RegOp && ldr0.op1 != null && ldr0.op1 instanceof ImmOp && ldr0.cond == null) { 
				
					REG base = ((RegOp) ldr0.op0).reg;
					
					if (base == REG.SP || base == REG.FP) {
						List<ASMLdr> ldrs = new ArrayList();
						ldrs.add(ldr0);
						
						for (int a = i - 1; a <= i; a++) {
							if (body.instructions.get(a).cond != null) break;
							if (body.instructions.get(a) instanceof ASMLdr) {
								ASMLdr ldr = (ASMLdr) body.instructions.get(a);
								
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
									body.instructions.remove(i - 2);
								
								MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.LDMFA;
								
								if (start < 0) 
									body.instructions.add(i - 2, new ASMSub(new RegOp(REG.R0), new RegOp(base), new ImmOp(-start)));
								else 
									body.instructions.add(i - 2, new ASMAdd(new RegOp(REG.R0), new RegOp(base), new ImmOp(start)));
									
								ASMMemBlock block = new ASMMemBlock(mode, false, new RegOp(REG.R0), regs, null);
							
								body.instructions.add(i - 1, block);
							}
						}
					}
				}
			}
		}
	}
	
	public void removeZeroOperands(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			ASMInstruction ins = body.instructions.get(i);
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
	
	public void replacePushPopWithBlockMemory(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				if (ASMMemBlock.checkInOrder(push.operands) && push.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
					body.instructions.set(i, new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(push.operands.size() * 4)));
					
					MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.STMEA;
					
					ASMMemBlock block = new ASMMemBlock(mode, false, new RegOp(REG.SP), push.operands, push.cond);
					body.instructions.add(i + 1, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOp> ops = new ArrayList();
					for (RegOp r : push.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && push.operands.size() > 1) {
						MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.STMFD;
						
						ASMMemBlock block = new ASMMemBlock(mode, true, new RegOp(REG.SP), ops, push.cond);
						body.instructions.set(i, block);
					}
				}
			}
			else if (body.instructions.get(i) instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) body.instructions.get(i);
				
				if (ASMMemBlock.checkInOrder(pop.operands) && pop.operands.size() > 1) {
					MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.LDMFD;
					
					ASMMemBlock block = new ASMMemBlock(mode, true, new RegOp(REG.SP), pop.operands, pop.cond);

					body.instructions.set(i, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOp> ops = new ArrayList();
					for (RegOp r : pop.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && pop.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
						body.instructions.set(i, new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(ops.size() * 4)));
						
						MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.LDMEA;
						
						ASMMemBlock block = new ASMMemBlock(mode, false, new RegOp(REG.SP), ops, pop.cond);
						body.instructions.add(i + 1, block);
					}
				}
			}
		}
	}
	
	public void markOpt() {
		OPT_DONE = true;
		opts++;
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
	
	public void deepRegPropagation(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				
				if (mov.optFlags.contains(OPT_FLAG.WRITEBACK) || mov.target.reg == REG.PC) continue;
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
				
					if (RegOp.toInt(reg) > 9 || RegOp.toInt(mov.target.reg) > 9) continue;

					boolean remove = true;
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						ASMInstruction ins = body.instructions.get(a);
						
						if (ins instanceof ASMBranch || ins instanceof ASMLabel || (ins instanceof ASMMov && ((ASMMov) ins).target.reg == REG.PC)) {
							remove = false;
							break;
						}
						
						if (ins instanceof ASMBinaryData) {
							ASMBinaryData d = (ASMBinaryData) ins;
							
							if (d.op0 != null && d.op0.reg == mov.target.reg && d.op0.reg != reg) {
								d.op0.reg = reg;
								markOpt();
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									markOpt();
								}
							}
						}
						else if (ins instanceof ASMCmp) {
							ASMCmp d = (ASMCmp) ins;
							
							if (d.op0.reg == mov.target.reg) {
								d.op0.reg = reg;
								markOpt();
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									markOpt();
								}
							}
						}
						else if (ins instanceof ASMStr) {
							ASMStr d = (ASMStr) ins;
							
							if (d.target.reg == mov.target.reg) {
								d.target.reg = reg;
								markOpt();
							}
							if (d.op0 instanceof RegOp) {
								RegOp op = (RegOp) d.op0;
								if (op.reg == mov.target.reg && op.reg != reg) {
									op.reg = reg;
									markOpt();
								}
							}
							if (d.op1 instanceof RegOp) {
								RegOp op1 = (RegOp) d.op1;
								if (op1.reg == mov.target.reg && op1.reg != reg) {
									op1.reg = reg;
									markOpt();
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
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	public void removeFPExchange(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				
				ASMPushStack push = null;
				if (body.instructions.get(i - 1) instanceof ASMPushStack) push = (ASMPushStack) body.instructions.get(i - 1);
				
				if (mov.target.reg == REG.FP && mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == REG.SP) {
					boolean clear = true;
					ASMMov mov0 = null;
					
					List<ASMInstruction> patch = new ArrayList();
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMMov) {
							mov0 = (ASMMov) body.instructions.get(a);
							if (mov0.target.reg == REG.SP && mov0.op1 instanceof RegOp && ((RegOp) mov0.op1).reg == REG.FP) {
								mov0 = (ASMMov) body.instructions.get(a);
								break;
							}
						}
						else patch.add(body.instructions.get(a));
						
						if (body.instructions.get(a) instanceof ASMPushStack && body.instructions.get(a).optFlags.contains(OPT_FLAG.STRUCT_INIT)) {
							clear = false;
							break;
						}
						else if (body.instructions.get(a) instanceof ASMBranch && ((ASMBranch) body.instructions.get(a)).type == BRANCH_TYPE.BL) {
							clear = false;
							break;
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
									body.instructions.remove(push);
									body.instructions.remove(push.popCounterpart);
								}
								
								body.instructions.remove(mov);
								
								this.patchFramePointerAddressing(patch, 4);
								
								this.patchFPtoSP(patch);
								
								i = body.instructions.indexOf(mov0) + 1;
								body.instructions.remove(mov0);
								
								markOpt();
							}
						}
					}
				}
			}
		}
	}
	
	public void implicitReturn(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
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
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a).equals(pop)) break;
						if (body.instructions.get(a) instanceof ASMBranch) {
							if (branch != null) {
								clear = false;
								break;
							}
							else {
								ASMBranch b = (ASMBranch) body.instructions.get(a);
								if (b.type == BRANCH_TYPE.BL) {
									branch = b;
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMMov && ((ASMMov) body.instructions.get(a)).target.reg == REG.PC) {
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
						
						i = body.instructions.indexOf(pop) + 1;
						
						if (push.operands.isEmpty()) {
							body.instructions.remove(push);
							body.instructions.remove(pop);
							i -= 2;
						}
						
						branch.type = BRANCH_TYPE.B;
						branch.optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
						
						markOpt();
					}
				}
			}
		}
	}
	
	public void popReturnDirect(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) body.instructions.get(i);
				if (branch.type == BRANCH_TYPE.BX && !branch.optFlags.contains(OPT_FLAG.BX_SEMI_EXIT) && body.instructions.get(i - 1) instanceof ASMPopStack) {
					ASMPopStack pop = (ASMPopStack) body.instructions.get(i - 1);
					
					if (pop.operands.size() > 0 && pop.operands.get(pop.operands.size() - 1).reg == REG.LR) {
						pop.operands.set(pop.operands.size() - 1, new RegOp(REG.PC));
						body.instructions.remove(i);
						i--;
						markOpt();
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
	
	private void removeFuncCleanStrict(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack && body.instructions.get(i).optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				boolean done = false;
				List<ASMInstruction> ins = new ArrayList();
				
				if (push.popCounterpart != null) {
					ASMPopStack pop = push.popCounterpart;
					
					int sub = 0;
					
					for (int k = i + 1; k < body.instructions.indexOf(push.popCounterpart) - 1; k++) ins.add(body.instructions.get(k));
					
					/* Check betweeen interval */
					for (int x = 0; x < push.operands.size(); x++) {
						RegOp reg = push.operands.get(x);
						
						/* Only for regular registers */
						if (RegOp.toInt(reg.reg) < 10) {
							
							/* Check if register was strictly not used */
							boolean regInteraction = false;
							for (int k = i + 1; k < body.instructions.indexOf(push.popCounterpart) - 1; k++) 
								regInteraction |= readsReg(body.instructions.get(k), reg.reg) || overwritesReg(body.instructions.get(k), reg.reg);
							
							if (!regInteraction) {
								push.operands.remove(x);
								pop.operands.remove(x);
								
								sub += 4;
								
								x--;
								markOpt();
								done = true;
							}
						}
					}
					
					/* Jump forward */
					i = body.instructions.indexOf(push.popCounterpart);
					
					if (push.operands.isEmpty()) {
						body.instructions.remove(push);
						body.instructions.remove(pop);
						markOpt();
						
						if (i > 0) i--;
					}
					
					if (done) patchFramePointerAddressing(ins, sub);
				}
			}
		}
	}
	
	private void bxSubstitution(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) body.instructions.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp label = (LabelOp) branch.target;
				
					int x = -1;
					
					/* Search for targeted label, indexOf is buggy in this situation :( */
					for (int a = 0; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMLabel) {
							ASMLabel l = (ASMLabel) body.instructions.get(a);
							if (l.name.equals(label.label.name)) {
								x = a;
								break;
							}
						}
					}
					
					if (x != -1 && body.instructions.get(x + 1) instanceof ASMBranch) {
						ASMBranch b0 = (ASMBranch) body.instructions.get(x + 1);
					
						if (b0.type == BRANCH_TYPE.BX) {
							for (int a = i; a < x + 1; a++) {
								if (body.instructions.get(a) instanceof ASMBranch) {
									branch = (ASMBranch) body.instructions.get(a);
									if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp && ((LabelOp) branch.target).label.equals(label.label)) {
										branch.type = BRANCH_TYPE.BX;
										branch.optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
										branch.target = b0.target.clone();
										
										markOpt();
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void popPcSubstitution(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch branch = (ASMBranch) body.instructions.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp label = (LabelOp) branch.target;
				
					int x = -1;
					
					/* Search for targeted label, indexOf is buggy in this situation :( */
					for (int a = 0; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMLabel) {
							ASMLabel l = (ASMLabel) body.instructions.get(a);
							if (l.name.equals(label.label.name)) {
								x = a;
								break;
							}
						}
					}
					
					if (x != -1 && body.instructions.get(x + 1) instanceof ASMPopStack) {
						ASMPopStack pop = (ASMPopStack) body.instructions.get(x + 1);
					
						if (ASMMemBlock.checkInOrder(pop.operands) || !CompilerDriver.optimizeFileSize) {
							if (pop.operands.get(pop.operands.size() - 1).reg == REG.PC) {
								for (int a = i; a < x + 1; a++) {
									if (body.instructions.get(a) instanceof ASMBranch) {
										branch = (ASMBranch) body.instructions.get(a);
										if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp && ((LabelOp) branch.target).label.equals(label.label)) {
											body.instructions.set(a, pop.clone());
											body.instructions.get(a).cond = branch.cond;
											markOpt();
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
	
	private void multPrecalc(AsNBody body) {
		for (int i = 2; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMult) {
				ASMMult mul = (ASMMult) body.instructions.get(i);
				
				REG mReg0 = mul.op0.reg;
				REG mReg1 = mul.op1.reg;
				
				ImmOp op0 = null;
				ImmOp op1 = null;
				
				boolean last = false;
				
				if (body.instructions.get(i - 2) instanceof ASMMov) {
					ASMMov mov = (ASMMov) body.instructions.get(i - 2);
					if (RegOp.toInt(mov.target.reg) < 3 && mov.op1 instanceof ImmOp) {
						if (mov.target.reg == mReg0) op0 = (ImmOp) mov.op1;
						if (mov.target.reg == mReg1) op1 = (ImmOp) mov.op1;
					}
				}
				
				if (body.instructions.get(i - 1) instanceof ASMMov) {
					ASMMov mov = (ASMMov) body.instructions.get(i - 1);
					if (RegOp.toInt(mov.target.reg) < 3 && mov.op1 instanceof ImmOp) {
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
						body.instructions.set(i, new ASMMov(new RegOp(mul.target.reg), new ImmOp(r)));
						
						/* Remove two movs */
						body.instructions.remove(i - 2);
						body.instructions.remove(i - 2);
						
						markOpt();
					}
				}
				else if (op1 != null) {
					/* Convert to LSL if last operand is given and is a power of 2 */
					if (Math.pow(2, (int) (Math.log(op1.value) / Math.log(2))) == op1.value && last) {
						int log = (int)(Math.log(op1.value) / Math.log(2)); 
						body.instructions.set(i, new ASMLsl(mul.target, mul.op0, new ImmOp(log)));
						body.instructions.remove(i - 1);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void mulToMlaMerge(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMult && body.instructions.get(i).cond == null) {
				ASMMult mul = (ASMMult) body.instructions.get(i - 1);
				
				if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i).cond == null) {
					ASMAdd add = (ASMAdd) body.instructions.get(i);
					
					if (mul.target.reg == add.op0.reg && add.op1 instanceof RegOp) {
						ASMMla mla = new ASMMla(add.target, mul.op0, mul.op1, (RegOp) add.op1);
						body.instructions.set(i - 1, mla);
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void removeIndirectMemOpAddressing(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMemOp) {
				ASMMemOp ldr = (ASMMemOp) body.instructions.get(i);
				
				if (ldr.op1 != null || !(ldr.op0 instanceof RegOp)) continue;
				
				REG dataTarget = null;
				RegOp op0 = null;
				Operand op1 = null;
				
				Shift shift = null;
				
				boolean negate = false;
				
				if (body.instructions.get(i - 1) instanceof ASMAdd) {
					ASMAdd add = (ASMAdd) body.instructions.get(i - 1);
					
					dataTarget = add.target.reg;
					op0 = add.op0.clone();
					op1 = add.op1.clone();
					
					shift = add.shift;
				}
				else if (body.instructions.get(i - 1) instanceof ASMSub) {
					ASMSub sub = (ASMSub) body.instructions.get(i - 1);
					
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
				else if (body.instructions.get(i - 1) instanceof ASMLsl) {
					/* 
					 * Will mostly target dereferencing operations. 
					 * Assumes that R10 is set to 0 at any given time. In most of the cases, this is given.
					 * In some special cases, like when calling the stack copy routine or the lambda head of a function,
					 * R10 is set temporary to the pc + 8. R10 is reset to 0 afterwards.
					 */
					ASMLsl lsl = (ASMLsl) body.instructions.get(i - 1);
					
					dataTarget = lsl.target.reg;
					op0 = new RegOp(REG.R10);
					
					op1 = lsl.op0.clone();
					shift = new Shift(SHIFT.LSL, lsl.op1.clone());
				}
				else continue;
				
				REG ldrTarget = ((RegOp) ldr.op0).reg;
					
				if (dataTarget != ldrTarget) continue;
				
				boolean clear = true;
				
				for (int a = i + 1; a < body.instructions.size(); a++) {
					if (readsReg(body.instructions.get(a), dataTarget)) {
						clear = false;
						break;
					}
					if (overwritesReg(body.instructions.get(a), dataTarget) && 
						!readsReg(body.instructions.get(a), dataTarget)) {
						break;
					}
					
					/* 
					 * In most cases, after a branch the sub instruction should
					 * not be relevant anymore, was only part of addressing.
					 * Still can be a potential problem causer, may need a stricter
					 * filter.
					 */
					if (body.instructions.get(a) instanceof ASMBranch) break;
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
					
					body.instructions.remove(i - 1);
					i--;
					markOpt();
				}
			}
		}
	}
	
	private void removeUnusedRegistersStrict(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMLabel && ((ASMLabel) body.instructions.get(i)).isFunctionLabel) {
				List<REG> probed = new ArrayList();
				
				for (int k = i; k < body.instructions.size(); k++) {
					if (body.instructions.get(k) instanceof ASMMov) {
						ASMMov mov = (ASMMov) body.instructions.get(k);
						REG reg = mov.target.reg;
						
						/*
						 *  Only probe first occurrence of assignment to register after 
						 *  function start. If mov instruction would not be first, but last
						 *  to assign to register, algorithm would delete the instruction
						 *  destroying the data flow.
						 */
						if (probed.contains(reg)) continue;
						else probed.add(reg);
						
						if (RegOp.toInt(reg) > 2 && reg != REG.R10 && reg != REG.FP && reg != REG.SP && reg != REG.LR && reg != REG.PC && reg != REG.R12) {
							boolean used = false;
							for (int a = k + 1; a < body.instructions.size(); a++) {
								if (body.instructions.get(a) instanceof ASMLabel && ((ASMLabel) body.instructions.get(a)).isFunctionLabel) {
									break;
								}
								else {
									used |= readsReg(body.instructions.get(a), reg);
								}
							}
							
							if (!used) {
								body.instructions.remove(k);
								i--;
								markOpt();
							}
						}
					}
				}
			}
		}
	}
	
	private void removeUnusedAssignment(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			REG reg = null;
			
			if (body.instructions.get(i) instanceof ASMBinaryData) {
				ASMBinaryData d = (ASMBinaryData) body.instructions.get(i);
				if (d.updateConditionField || d.cond != null) continue;
				reg = d.target.reg;
			}
			else if (body.instructions.get(i) instanceof ASMLdr) {
				ASMLdr d = (ASMLdr) body.instructions.get(i);
				if (d.cond != null) continue;
				reg = d.target.reg;
			}
			else if (body.instructions.get(i) instanceof ASMLdrStack) {
				ASMLdrStack d = (ASMLdrStack) body.instructions.get(i);
				if (d.cond != null) continue;
				reg = d.target.reg;
			}
			
			if (reg == null) continue;
			
			if (RegOp.toInt(reg) < 3) {
				for (int a = i + 1; a < body.instructions.size(); a++) {
					ASMInstruction ins = body.instructions.get(a);
					
					if (readsReg(ins, reg) || ins instanceof ASMBranch || overwritesReg(ins, REG.PC)) {
						break;
					}
					else if (overwritesReg(ins, reg) && !readsReg(ins, reg)) {
						body.instructions.remove(i);
						i--;
						markOpt();
						break;
					}
				}
			}
			else if (RegOp.toInt(reg) < 10) {
				for (int a = i + 1; a < body.instructions.size(); a++) {
					ASMInstruction ins = body.instructions.get(a);
					
					if (readsReg(ins, reg) || ins.optFlags.contains(OPT_FLAG.LOOP_BRANCH)) {
						break;
					}
					else if (ins instanceof ASMLabel && ((ASMLabel) ins).isFunctionLabel || a == body.instructions.size() - 1) {
						body.instructions.remove(i);
						i--;
						markOpt();
						break;
					}
				}
			}
		}
	}
	
	private void pushPopRelocation(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				if (push.operands.size() == 1 && !push.optFlags.contains(OPT_FLAG.FUNC_CLEAN) && !push.optFlags.contains(OPT_FLAG.STRUCT_INIT)) {
					
					body.instructions.remove(i);
					
					REG reg = push.operands.get(0).reg;
					
					boolean afterFPExchange = false;
					int line = i;
					while (true) {
						if (overwritesReg(body.instructions.get(line), reg) || 
								body.instructions.get(line) instanceof ASMBranch || body.instructions.get(line) instanceof ASMLabel || 
								body.instructions.get(line) instanceof ASMStackOp || body.instructions.get(line) instanceof ASMPushStack ||
								body.instructions.get(line) instanceof ASMLdr || body.instructions.get(line) instanceof ASMPopStack) {
							break;
						}
						else if (readsReg(body.instructions.get(line), REG.SP)) break;
						else {
							if (body.instructions.get(line) instanceof ASMMov) {
								ASMMov mov = (ASMMov) body.instructions.get(line);
								if (mov.target.reg == REG.SP && mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == REG.FP) {
									afterFPExchange = true;
								}
							}
							line++;
						}
					}
					
					if (line != i || afterFPExchange) markOpt();
					if (!afterFPExchange) body.instructions.add(line, push);
				}
			}
		}
		
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) body.instructions.get(i);
				
				if (pop.operands.size() == 1 && !pop.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
					body.instructions.remove(i);
					
					REG reg = pop.operands.get(0).reg;
					
					int line = i - 1;
					while (true) {
						if (readsReg(body.instructions.get(line), reg) || overwritesReg(body.instructions.get(line), reg) ||
								body.instructions.get(line) instanceof ASMBranch || body.instructions.get(line) instanceof ASMLabel || 
								body.instructions.get(line) instanceof ASMStackOp || 
								body.instructions.get(line) instanceof ASMStr || body.instructions.get(line) instanceof ASMPushStack ) {
							break;
						}
						
						line--;
					}
					
					if (line != i - 1) markOpt();
					body.instructions.add(line + 1, pop);
					
					i = line + 2;
				}
			}
		}
	}
	
	private void defragmentAdditions(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i - 1) instanceof ASMAdd) {
				ASMAdd add0 = (ASMAdd) body.instructions.get(i - 1);
				ASMAdd add1 = (ASMAdd) body.instructions.get(i);
				if (add0.target.reg == add1.target.reg && add0.target.reg == add1.op0.reg && 
						add0.op1 instanceof ImmOp && add1.op1 instanceof ImmOp) {
					ImmOp op0 = (ImmOp) add0.op1;
					ImmOp op1 = (ImmOp) add1.op1;
					
					op0.value += op1.value;
					body.instructions.remove(i);
					i--;
					markOpt();
				}
			}
		}
		
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i - 1) instanceof ASMAdd) {
				ASMAdd add0 = (ASMAdd) body.instructions.get(i - 1);
				ASMAdd add1 = (ASMAdd) body.instructions.get(i);
				
				if (add0.target.reg == add1.op0.reg && add0.op1 instanceof ImmOp && add1.op1 instanceof ImmOp) {
					ImmOp op0 = (ImmOp) add0.op1;
					ImmOp op1 = (ImmOp) add1.op1;
					
					op1.value = op0.value + op1.value;
					add1.op0.reg = add0.op0.reg;
					
					op0.value += op1.value;
					body.instructions.remove(i - 1);
					i--;
					markOpt();
				}
			}
		}
	}
	
	private void shiftBy0IsMov(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			ASMInstruction ins = body.instructions.get(i);
			if (ins instanceof ASMLsl) {
				ASMLsl lsl = (ASMLsl) ins;
				if (lsl.op1 != null && lsl.op1 instanceof ImmOp) {
					ImmOp imm = (ImmOp) lsl.op1;
					if (imm.value == 0) {
						body.instructions.set(i, new ASMMov(lsl.target, lsl.op0, lsl.cond));
						markOpt();
					}
				}
			}
			else if (ins instanceof ASMLsr) {
				ASMLsr lsr = (ASMLsr) ins;
				if (lsr.op1 != null && lsr.op1 instanceof ImmOp) {
					ImmOp imm = (ImmOp) lsr.op1;
					if (imm.value == 0) {
						body.instructions.set(i, new ASMMov(lsr.target, lsr.op0, lsr.cond));
						markOpt();
					}
				}
			}
		}
	}
	
	private void defragmentDeltas(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i - 1) instanceof ASMSub) {
				ASMSub sub = (ASMSub) body.instructions.get(i - 1);
				ASMAdd add = (ASMAdd) body.instructions.get(i);
				
				if (sub.target.reg == add.target.reg && add.target.reg == add.op0.reg && 
						sub.op1 instanceof ImmOp && add.op1 instanceof ImmOp) {
					ImmOp subOp = (ImmOp) sub.op1;
					ImmOp addOp = (ImmOp) add.op1;
					
					if (subOp.value >= addOp.value) {
						subOp.value -= addOp.value;
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void additionCommutative(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				ASMAdd add = (ASMAdd) body.instructions.get(i);
				
				if (mov.target.reg == add.op0.reg && mov.op1 instanceof ImmOp && add.op1 instanceof RegOp && add.shift == null) {
					RegOp op1 = (RegOp) add.op1;
					add.op1 = mov.op1;
					add.op0 = op1;
					
					body.instructions.remove(i - 1);
					i--;
					markOpt();
				}
			}
		}
		
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd && body.instructions.get(i - 1) instanceof ASMLsl) {
				ASMLsl lsl = (ASMLsl) body.instructions.get(i - 1);
				ASMAdd add = (ASMAdd) body.instructions.get(i);
				
				if (lsl.target.reg == add.op0.reg && lsl.op1 instanceof ImmOp && add.op1 instanceof RegOp && add.shift == null) {
					RegOp op1 = (RegOp) add.op1;
					add.op1 = lsl.op0;
					
					add.shift = new Shift(SHIFT.LSL, lsl.op1);
					
					add.op0 = op1;
					
					body.instructions.remove(i - 1);
					i--;
					markOpt();
				}
			}
		}
	}

	private void subtractionSemiCommutative(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMSub && body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				ASMSub sub = (ASMSub) body.instructions.get(i);
				
				if (mov.target.reg == sub.op0.reg && mov.op1 instanceof ImmOp && sub.op1 instanceof RegOp) {
					RegOp op1 = (RegOp) sub.op1;
					
					body.instructions.set(i, new ASMRsb(sub.target, op1, mov.op1));
					
					body.instructions.remove(i - 1);
					i--;
					markOpt();
				}
			}
		}
	}
	
	private void removeZeroInstruction(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMAdd) {
				ASMAdd add = (ASMAdd) body.instructions.get(i);
				if (add.target.reg == add.op0.reg && add.op1 instanceof ImmOp && !add.updateConditionField) {
					ImmOp imm = (ImmOp) add.op1;
					if (imm.value == 0) {
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
			else if (body.instructions.get(i) instanceof ASMSub) {
				ASMSub sub = (ASMSub) body.instructions.get(i);
				if (sub.target.reg == sub.op0.reg && sub.op1 instanceof ImmOp && !sub.updateConditionField) {
					ImmOp imm = (ImmOp) sub.op1;
					if (imm.value == 0) {
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void removeUnnessesaryPushPop(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				boolean remove = false;
				
				if (push.operands.size() == 1) {
					REG reg = ((RegOp) push.operands.get(0)).reg;
					for (int a = i; a < body.instructions.size(); a++) {
						ASMInstruction ins = body.instructions.get(a);
						
						if (ins instanceof ASMPopStack) {
							ASMPopStack pop = (ASMPopStack) ins;
							
							if (pop.operands.size() == 1 && !pop.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
								RegOp op0 = (RegOp) pop.operands.get(0);
								if (op0.reg == reg) {
									body.instructions.remove(a);
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
					body.instructions.remove(i);
					i--;
				}
			}
		}
	}
	
	private void removeIndirectPushPopAssign(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				if (push.operands.size() == 1) {
					REG pushReg = push.operands.get(0).reg;
					
					/* Search for pop Counterpart */
					boolean found = false;
					ASMPopStack pop = null;
					int end = 0;
					for (int a = i + 1; a < body.instructions.size(); a++) {
						ASMInstruction ins = body.instructions.get(a);
						
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
							if (overwritesReg(body.instructions.get(a), push.operands.get(0).reg)) {
								replace = false;
								break;
							}
						}
						
						if (replace) {
							body.instructions.set(body.instructions.indexOf(pop), new ASMMov(new RegOp(newReg), new RegOp(pushReg)));
							body.instructions.remove(push);
							
							markOpt();
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeExpressionIndirectTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					if (reg == REG.R0 || reg == REG.R1 || reg == REG.R2) {
						
						boolean replace = true;
						
						if (body.instructions.get(i + 1) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) body.instructions.get(i + 1);
							if (cmp.op0.reg == reg) replace = false;
						}
						
						boolean patchUp = false;
						if (body.instructions.get(i - 1) instanceof ASMBinaryData && !(body.instructions.get(i - 1) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) body.instructions.get(i - 1);
							if (data.target.reg == reg && replace) {
								data.target = mov.target;
								markOpt();
								patchUp = true;
							}
						}
						else if (body.instructions.get(i - 1) instanceof ASMMult) {
							ASMMult mul = (ASMMult) body.instructions.get(i - 1);
							if (mul.target.reg == reg && replace) {
								mul.target = mov.target;
								markOpt();
								patchUp = true;
							}
						}
						
						if (patchUp) body.instructions.remove(i);
					}
				}
			}
		}
	}
	
	private void removeLdrIndirectTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					
					/* Only perform action if target is a operand register. */
					if (RegOp.toInt(reg) > 2) continue;
					
					if (body.instructions.get(i - 1) instanceof ASMLdrStack) {
						ASMLdrStack ldr = (ASMLdrStack) body.instructions.get(i - 1);
						
						if (ldr.target.reg == reg) {
							ldr.target.reg = mov.target.reg;
							markOpt();
							body.instructions.remove(i);
							i--;
						}
					}
					else if (body.instructions.get(i - 1) instanceof ASMLdr) {
						ASMLdr ldr = (ASMLdr) body.instructions.get(i - 1);
						
						if (ldr.target.reg == reg) {
							ldr.target.reg = mov.target.reg;
							markOpt();
							body.instructions.remove(i);
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeStrOperandIndirectTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				
				if (mov.op1 instanceof RegOp) {
					REG reg = ((RegOp) mov.op1).reg;
					
					if (body.instructions.get(i) instanceof ASMStrStack) {
						ASMStrStack str = (ASMStrStack) body.instructions.get(i);
						
						if (str.target.reg == mov.target.reg) {
							str.target.reg = reg;
							markOpt();
							body.instructions.remove(i - 1);
							i--;
						}
					}
					else if (body.instructions.get(i) instanceof ASMStr) {
						ASMStr str = (ASMStr) body.instructions.get(i);
						
						if (str.target.reg == mov.target.reg) {
							str.target.reg = reg;
							markOpt();
							body.instructions.remove(i - 1);
							i--;
						}
					}
				}
			}
		}
	}
	
	private void removeOperandIndirectTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				/* For all operand regs */
				for (int a = 0; a < 2; a++) {
					if (mov.target.reg == RegOp.toReg(a) && mov.op1 instanceof RegOp) {
						if (body.instructions.get(i) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) body.instructions.get(i);
							if (cmp.op0 != null && cmp.op0.reg == RegOp.toReg(a)) {
								/* Replace */
								cmp.op0 = (RegOp) mov.op1;
								markOpt();
								
								body.instructions.remove(i - 1);
								i--;
							}
							else if (cmp.op1 != null && cmp.op1 instanceof RegOp && ((RegOp) cmp.op1).reg == RegOp.toReg(a)) {
								/* Replace */
								cmp.op1 = (RegOp) mov.op1;
								markOpt();
								
								body.instructions.remove(i - 1);
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
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMStr && body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				ASMStr str = (ASMStr) body.instructions.get(i);
				
				if (str.target.reg == mov.target.reg && mov.op1 instanceof RegOp) {
					boolean clear = true;
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (readsReg(body.instructions.get(a), mov.target.reg)) {
							clear = false;
							break;
						}
						
						if (overwritesReg(body.instructions.get(a), mov.target.reg))
							break;
					}
					
					if (clear) {
						body.instructions.remove(i - 1);
						str.target = (RegOp) mov.op1;
						i--;
						markOpt();
					}
				}
			}
		}
		
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				for (int a = 0; a < 10; a++) {
					if (mov.target.reg == RegOp.toReg(a) && mov.op1 instanceof RegOp) {
						RegOp target = (RegOp) mov.op1;
						
						/* Writeback flag is set, cannot substitute, since mov copies operand */
						if (body.instructions.get(i).optFlags.contains(OPT_FLAG.WRITEBACK)) {
							break;
						}
						
						if (body.instructions.get(i) instanceof ASMBinaryData && !(body.instructions.get(i) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) body.instructions.get(i);
							boolean remove = false;
							if (data.op0 != null && data.op0.reg == RegOp.toReg(a)) {
								/* Replace */
								data.op0 = target;
								markOpt();
								remove = true;
							}
							
							if (data.op1 != null && data.op1 instanceof RegOp && ((RegOp) data.op1).reg == RegOp.toReg(a)) {
								/* Replace */
								data.op1 = target;
								markOpt();
								remove = true;
							}
							
							if (remove && a < 3) {
								body.instructions.remove(i - 1);
								i--;
							}
						}
						else if (body.instructions.get(i) instanceof ASMMult) {
							ASMMult mul = (ASMMult) body.instructions.get(i);
							boolean remove = false;
							if (mul.op0 != null && mul.op0.reg == RegOp.toReg(a)) {
								/* Replace */
								mul.op0 = target;
								markOpt();
								remove = true;
							}
							
							if (mul.op1 != null && mul.op1 instanceof RegOp && ((RegOp) mul.op1).reg == RegOp.toReg(a)) {
								/* Replace */
								mul.op1 = target;
								markOpt();
								remove = true;
							}
							
							if (remove && a < 3) {
								body.instructions.remove(i - 1);
								i--;
							}
						}
						else if (i + 1 < body.instructions.size() && body.instructions.get(i + 1) instanceof ASMMult) {
							
							ASMInstruction ins0 = body.instructions.get(i);
							if (!(ins0 instanceof ASMBranch || ins0 instanceof ASMLabel || 
								 ins0 instanceof ASMMemOp || ins0 instanceof ASMStackOp || 
								 ins0 instanceof ASMPushStack || ins0 instanceof ASMPopStack)) {
								
								ASMMult mul = (ASMMult) body.instructions.get(i + 1);
								boolean remove = false;
								
								if (overwritesReg(ins0, target.reg)) continue;
								
								if (mul.op0 != null && mul.op0.reg == RegOp.toReg(a)) {
									/* Replace */
									mul.op0 = target;
									markOpt();
									remove = true;
								}
								
								if (mul.op1 != null && mul.op1 instanceof RegOp && mul.op1.reg == RegOp.toReg(a)) {
									/* Replace */
									mul.op1 = target;
									markOpt();
									remove = true;
								}
								
								if (remove && a < 3 && !mov.optFlags.contains(OPT_FLAG.WRITEBACK)) {
									body.instructions.remove(i - 1);
									i--;
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void removeImplicitPushPopTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i - 1);
				
				if (mov.target.reg == REG.R0 && mov.op1 instanceof RegOp) {
					if (body.instructions.get(i) instanceof ASMPushStack) {
						ASMPushStack push = (ASMPushStack) body.instructions.get(i);
						
						if (push.operands.size() == 1 && push.operands.get(0).reg == mov.target.reg) {
							push.operands.get(0).reg = ((RegOp) mov.op1).reg;
							body.instructions.remove(i - 1);
							i--;
							markOpt();
						}
					}
				}
			}
		}
	}
	
	private void removeBranchesBeforeLabelToLabel(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMBranch && body.instructions.get(i) instanceof ASMLabel) {
				ASMBranch branch = (ASMBranch) body.instructions.get(i - 1);
				ASMLabel label = (ASMLabel) body.instructions.get(i);
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOp) {
					LabelOp op = (LabelOp) branch.target;
					
					if (op.label.name.equals(label.name)) {
						body.instructions.remove(i - 1);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void removeDoubleCrossing(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov && body.instructions.get(i) instanceof ASMMov) {
				ASMMov move0 = (ASMMov) body.instructions.get(i - 1);
				ASMMov move1 = (ASMMov) body.instructions.get(i);
				if (move0.op1 instanceof RegOp && move1.op1 instanceof RegOp) {
					RegOp op0 = (RegOp) move0.op1;
					RegOp op1 = (RegOp) move1.op1;
					if (op0.reg == REG.FP || op1.reg == REG.FP) continue;
					
					if (op0.reg == move1.target.reg && move0.target.reg == op1.reg) {
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
			else if (body.instructions.get(i) instanceof ASMMov) {
				/* Remove identity mov */
				ASMMov mov = (ASMMov) body.instructions.get(i);
				if (mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == mov.target.reg) {
					body.instructions.remove(i);
					i--;
					markOpt();
				}
			}
		}
	}
	
	private void removeMovId(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				/* Remove identity mov */
				ASMMov mov = (ASMMov) body.instructions.get(i);
				if (mov.op1 instanceof RegOp && ((RegOp) mov.op1).reg == mov.target.reg) {
					body.instructions.remove(i);
					i--;
					markOpt();
				}
			}
		}
	}
	
	private void constantOperandPropagation(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov move = (ASMMov) body.instructions.get(i);
				
				if (move.cond != null) continue;
				
				if (move.op1 instanceof ImmOp) {
					int val = ((ImmOp) move.op1).value;
					REG target = move.target.reg;
					
					/* Set to false if reg is read */
					boolean clear = true;
					
					boolean hardClear = false;
					
					/* Break is reg is overwritten */
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMBranch ||
							body.instructions.get(a) instanceof ASMLabel ||
							body.instructions.get(a) instanceof ASMMov && ((ASMMov) body.instructions.get(a)).target.reg == REG.PC) {
							clear = false;
							break;
						}
						
						if (body.instructions.get(a) instanceof ASMComment) {
							continue;
						}
						/* Substitute immediate value into move */
						else if (body.instructions.get(a) instanceof ASMMov) {
							ASMMov move0 = (ASMMov) body.instructions.get(a);
							
							if (move0.target.reg == target) {
								if (move0.cond != null) clear = false;
								break;
							}
							
							if (move0.op1 instanceof RegOp && ((RegOp) move0.op1).reg == target) {
								move0.op1 = new ImmOp(val);
								markOpt();
							}
						}
						else if (body.instructions.get(a) instanceof ASMMvn) {
							ASMMvn move0 = (ASMMvn) body.instructions.get(a);
							
							if (move0.target.reg == target) {
								if (move0.cond != null) clear = false;
								break;
							}
							
							if (move0.op1 instanceof RegOp && ((RegOp) move0.op1).reg == target) {
								move0.op1 = new ImmOp(val);
								markOpt();
							}
						}
						else if (body.instructions.get(a) instanceof ASMBinaryData) {
							ASMBinaryData dataP = (ASMBinaryData) body.instructions.get(a);
							
							if (dataP.optFlags.contains(OPT_FLAG.WRITEBACK)) {
								clear = false;
								break;
							}
							
							if (dataP.op1 instanceof RegOp && ((RegOp) dataP.op1).reg == target) {
								dataP.op1 = new ImmOp(val);
								markOpt();
								
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
									
									body.instructions.set(a, ins);
									markOpt();
									a -= 2;
								}
								else clear = false;
							}
							if (dataP.target.reg == target) {
								break;
							}
						}
						else if (body.instructions.get(a) instanceof ASMMult) {
							ASMMult mult = (ASMMult) body.instructions.get(a);
							
							if (mult.op0.reg == target || mult.op1.reg == target) {
								clear = false;
							}
							
							if (mult.target.reg == target) {
								break;
							}
						}
						else if (body.instructions.get(a) instanceof ASMMla) {
							ASMMla mla = (ASMMla) body.instructions.get(a);
							
							if (mla.op0.reg == target || mla.op1.reg == target || mla.op2.reg == target) {
								clear = false;
							}
							
							if (mla.target.reg == target) {
								break;
							}
						}
						else if (body.instructions.get(a) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) body.instructions.get(a);
							
							if (cmp.op1 instanceof RegOp && ((RegOp) cmp.op1).reg == target) {
								cmp.op1 = new ImmOp(val);
								markOpt();
							}
							
							if (cmp.op0.reg == target) {
								clear = false;
							}
						}
						else if (body.instructions.get(a) instanceof ASMPushStack) {
							ASMPushStack p = (ASMPushStack) body.instructions.get(a);
							for (RegOp r : p.operands) {
								if (r.reg == target) {
									clear = false;
									break;
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMPopStack) {
							ASMPopStack p = (ASMPopStack) body.instructions.get(a);
							boolean end = false;
							for (RegOp r : p.operands) {
								if (r.reg == target) {
									end = true;
									break;
								}
							}
							
							if (end) break;
						}
						else if (body.instructions.get(a) instanceof ASMLdrStack) {
							ASMLdrStack p = (ASMLdrStack) body.instructions.get(a);
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
									markOpt();
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMLdr) {
							ASMLdr p = (ASMLdr) body.instructions.get(a);
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
									markOpt();
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMStr) {
							ASMStr p = (ASMStr) body.instructions.get(a);
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
									markOpt();
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMStrStack) {
							ASMStrStack p = (ASMStrStack) body.instructions.get(a);
							if (p.target.reg == target) {
								clear = false;
								break;
							}
							
							if (p.op0.reg == target || (p.op1 instanceof RegOp && ((RegOp) p.op1).reg == target)) {
								clear = false;
							}
						}
						else if (body.instructions.get(a) instanceof ASMHardcode) {
							clear = false;
							break;
						}
						else if (body.instructions.get(a) instanceof ASMSeperator) {
							/* Do nothing */
						}
						else {
							clear = false;
							new Message("ASMOPT -> ConstOp propagation : Not available " + body.instructions.get(a).getClass().getName(), LogPoint.Type.WARN);
						}
						
					}
					
					if (clear || hardClear) {
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
		}
	}
	
	private void clearUnusedLabels(AsNBody body) {
		List<ASMLabel> usedLabels = new ArrayList();
		
		for (int i = 1; i < body.instructions.size(); i++) {
			ASMInstruction ins = body.instructions.get(i);
			if (ins instanceof ASMLabel) {
				ASMLabel label = (ASMLabel) ins;
				/* Label is used by default if its a function header or a data label */
				if (label.isFunctionLabel || label instanceof ASMDataLabel) usedLabels.add((ASMLabel) ins);
			}
			if (ins instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) ins;
				if (b.target instanceof LabelOp) usedLabels.add(((LabelOp) b.target).label);
			}
		}
		
		for (int i = 1; i < body.instructions.size(); i++) {
			ASMInstruction ins = body.instructions.get(i);
			if (ins instanceof ASMLabel) {
				if (!usedLabels.contains(ins)) {
					body.instructions.remove(i);
					markOpt();
					while (i < body.instructions.size() && !(ins instanceof ASMLabel)) {
						body.instructions.remove(i);
					}
					i--;
				}
			}
		}
	}
	
	private void clearInstructionsAfterBranch(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) body.instructions.get(i);
				if (b.cond == null && b.type != BRANCH_TYPE.BL && !b.optFlags.contains(OPT_FLAG.SYS_JMP)) {
					while (i < body.instructions.size() - 1 && !(body.instructions.get(i + 1) instanceof ASMSeperator)) {
						if (body.instructions.get(i + 1) instanceof ASMLabel && !(body.instructions.get(i + 1) instanceof ASMDataLabel)) break;
						if (body.instructions.get(i + 1) instanceof ASMComment || body.instructions.get(i + 1) instanceof ASMDataLabel) {
							i++;
							continue;
						}
						body.instructions.remove(i + 1);
						markOpt();
					}
				}
			}
		}
	}
	
	private void removeDoubleLabels(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMLabel && !(body.instructions.get(i) instanceof ASMDataLabel)) {
				ASMLabel l1 = (ASMLabel) body.instructions.get(i);
				
				if (body.instructions.get(i - 1) instanceof ASMLabel && !(body.instructions.get(i - 1) instanceof ASMDataLabel)) {
					ASMLabel l0 = (ASMLabel) body.instructions.get(i - 1);
					
					l1.name = l0.name;
					body.instructions.remove(i);
					i--;
					markOpt();
				}
			}
		}
	}
	
} 
