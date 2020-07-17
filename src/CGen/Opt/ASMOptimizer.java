package CGen.Opt;

import java.util.ArrayList;
import java.util.List;

import Exc.SNIPS_EXCEPTION;
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
import Imm.ASM.Processing.ASMBinaryData.SHIFT_TYPE;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
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
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Logging.Message;
import Util.Logging.Message.Type;

/**
 * This optimizer can simplify a sequence of asm instructions. By doing so
 * the optimizer will not change the behaviour of the program itself.<br>
 * The optimizer follows an internal logic of the Compiler, so this optimizer
 * will not work for any Assembly program, since it may violate the compiler internal
 * logic. This will lead to context changing optimizations.
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
			 * add r0, r0, #2
			 * add r0, r0, #4
			 * Replace with:
			 * add r0, r0, #6
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
				 * Check if a bx lr statement is directley after the func clean pop. If the last
				 * pop operand is a lr, substitute pc in the operand and remove the bx lr.
				 */
				this.popReturnDirect(body);
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

		/* Finishing touches, no iteration required */
		this.popPcSubstitution(body);
		this.clearUnusedLabels(body);
		
		/* Replace large push/pop operations with ldm/stm */
		this.replacePushPopWithBlockMemory(body);
		
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
	
	public void replacePushPopWithBlockMemory(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i);
				
				if (ASMMemBlock.checkInOrder(push.operands) && push.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
					body.instructions.set(i, new ASMSub(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(push.operands.size() * 4)));
					
					MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.STMEA;
					
					ASMMemBlock block = new ASMMemBlock(mode, false, new RegOperand(REGISTER.SP), push.operands, push.cond);
					body.instructions.add(i + 1, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOperand> ops = new ArrayList();
					for (RegOperand r : push.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && push.operands.size() > 1) {
						MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.STMFD;
						
						ASMMemBlock block = new ASMMemBlock(mode, true, new RegOperand(REGISTER.SP), ops, push.cond);
						body.instructions.set(i, block);
					}
				}
			}
			else if (body.instructions.get(i) instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) body.instructions.get(i);
				
				if (ASMMemBlock.checkInOrder(pop.operands) && pop.operands.size() > 1) {
					MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.LDMFD;
					
					ASMMemBlock block = new ASMMemBlock(mode, true, new RegOperand(REGISTER.SP), pop.operands, pop.cond);

					body.instructions.set(i, block);
				}
				else {
					/* Operands are flipped here */
					List<RegOperand> ops = new ArrayList();
					for (RegOperand r : pop.operands) ops.add(0, r);
					
					if (ASMMemBlock.checkInOrder(ops) && pop.operands.size() > 2 && !CompilerDriver.optimizeFileSize) {
						body.instructions.set(i, new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(ops.size() * 4)));
						
						MEM_BLOCK_MODE mode = MEM_BLOCK_MODE.LDMEA;
						
						ASMMemBlock block = new ASMMemBlock(mode, false, new RegOperand(REGISTER.SP), ops, pop.cond);
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
	public static boolean overwritesReg(ASMInstruction ins, REGISTER reg) {
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
			return (load.memOp == MEM_OP.POST_WRITEBACK || load.memOp == MEM_OP.PRE_WRITEBACK) && (load.op1 instanceof RegOperand && ((RegOperand) load.op1).reg == reg);
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
		else throw new SNIPS_EXCEPTION("Cannot check if instruction overwrites register: " + ins.getClass().getName());
	}
	
	/**
	 * Check if given register is read by given instruction.
	 */
	public static boolean readsReg(ASMInstruction ins, REGISTER reg) {
		if (ins instanceof ASMBinaryData) {
			ASMBinaryData data = (ASMBinaryData) ins;
			return (data.op0 != null && data.op0.reg == reg) || (data.op1 instanceof RegOperand && ((RegOperand) data.op1).reg == reg);
		}
		else if (ins instanceof ASMCmp) {
			ASMCmp cmp = (ASMCmp) ins;
			return cmp.op0.reg == reg || (cmp.op1 instanceof RegOperand && ((RegOperand) cmp.op1).reg == reg);
		}
		else if (ins instanceof ASMStr) {
			ASMStr str = (ASMStr) ins;
			return str.target.reg == reg || (str.op0 instanceof RegOperand && ((RegOperand) str.op0).reg == reg) || (str.op1 instanceof RegOperand && ((RegOperand) str.op1).reg == reg);
		}
		else if (ins instanceof ASMMemOp) {
			ASMMemOp op = (ASMMemOp) ins;
			return (op.op0 instanceof RegOperand && ((RegOperand) op.op0).reg == reg) || (op.op1 instanceof RegOperand && ((RegOperand) op.op1).reg == reg);
		}
		else if (ins instanceof ASMLdrStack) {
			ASMLdrStack ldr = (ASMLdrStack) ins;
			return (ldr.op0 instanceof RegOperand && ((RegOperand) ldr.op0).reg == reg) || (ldr.op1 instanceof RegOperand && ((RegOperand) ldr.op1).reg == reg);
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
			return str.target.reg == reg || str.op0.reg == reg || (str.op1 != null && str.op1 instanceof RegOperand && ((RegOperand) str.op1).reg == reg);
		}
		else if (ins instanceof ASMPushStack) {
			ASMPushStack push = (ASMPushStack) ins;
			for (RegOperand r : push.operands) if (r.reg == reg) return true;
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
		else throw new SNIPS_EXCEPTION("Cannot check if instruction reads register: " + ins.getClass().getName());
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
				if (add.target.reg == REGISTER.SP && add.target.reg == REGISTER.SP && add.op1 instanceof ImmOperand) {
					pushed -= ((ImmOperand) add.op1).value >> 2;
				}
			}
			else if (ins instanceof ASMSub) {
				ASMSub sub = (ASMSub) ins;
				if (sub.target.reg == REGISTER.SP && sub.target.reg == REGISTER.SP && sub.op1 instanceof ImmOperand) {
					pushed += ((ImmOperand) sub.op1).value >> 2;
				}
			}
			else if (ins instanceof ASMMemOp) {
				
			}
			else if (ins instanceof ASMStackOp && (((ASMStackOp) ins).memOp == MEM_OP.PRE_NO_WRITEBACK || ((ASMStackOp) ins).op1 == null)) {
				
			}
			else {
				if (readsReg(ins, REGISTER.SP) || overwritesReg(ins, REGISTER.SP)) {
					throw new SNIPS_EXCEPTION("Cannot SP info from: " + ins.getClass().getName());
				}
			}
		}
		
		return pushed;
	}
	
	public void patchFPtoSP(List<ASMInstruction> patch) {
		for (ASMInstruction ins : patch) {
			if (ins instanceof ASMBinaryData) {
				ASMBinaryData d = (ASMBinaryData) ins;
				if (d.target.reg == REGISTER.FP) d.target.reg = REGISTER.SP;
				if (d.op0.reg == REGISTER.FP) d.op0.reg = REGISTER.SP;
				if (d.op1 instanceof RegOperand) {
					RegOperand op = (RegOperand) d.op1;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMCmp) {
				ASMCmp d = (ASMCmp) ins;
				if (d.op0.reg == REGISTER.FP) d.op0.reg = REGISTER.SP;
				if (d.op1 instanceof RegOperand) {
					RegOperand op = (RegOperand) d.op1;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMBranch) {
				ASMBranch d = (ASMBranch) ins;
				if (d.target instanceof RegOperand) {
					RegOperand op = (RegOperand) d.target;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp memOp = (ASMMemOp) ins;
				if (memOp.target.reg == REGISTER.FP) memOp.target.reg = REGISTER.SP;
				
				if (memOp.op1 instanceof PatchableImmOperand) {
					PatchableImmOperand op = (PatchableImmOperand) memOp.op1;
					int words = this.countPushedWords(patch, memOp);
					memOp.op1 = new ImmOperand(op.patchedValue + (words * 4));
				}
				else if (memOp.op1 instanceof ImmOperand && memOp.op0 instanceof RegOperand && ((RegOperand) memOp.op0).reg == REGISTER.FP) {
					ImmOperand op = (ImmOperand) memOp.op1;
					int words = this.countPushedWords(patch, memOp);
					memOp.op1 = new ImmOperand(op.value + (words * 4));
				}
				
				if (memOp.op0 instanceof RegOperand) {
					RegOperand op = (RegOperand) memOp.op0;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
				if (memOp.op1 instanceof RegOperand) {
					RegOperand op = (RegOperand) memOp.op1;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMLdrStack) {
				ASMLdrStack ldr = (ASMLdrStack) ins;
				if (ldr.target.reg == REGISTER.FP) ldr.target.reg = REGISTER.SP;
				
				if (ldr.op1 instanceof PatchableImmOperand) {
					PatchableImmOperand op = (PatchableImmOperand) ldr.op1;
					int words = this.countPushedWords(patch, ldr);
					ldr.op1 = new ImmOperand(op.patchedValue + (words * 4));
				}
				else if (ldr.op1 instanceof ImmOperand && ldr.op0 instanceof RegOperand && ((RegOperand) ldr.op0).reg == REGISTER.FP) {
					ImmOperand op = (ImmOperand) ldr.op1;
					int words = this.countPushedWords(patch, ldr);
					ldr.op1 = new ImmOperand(op.value + (words * 4));
				}
				
				if (ldr.op0 instanceof RegOperand) {
					RegOperand op = (RegOperand) ldr.op0;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
				if (ldr.op1 instanceof RegOperand) {
					RegOperand op = (RegOperand) ldr.op1;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMMult) {
				ASMMult mul = (ASMMult) ins;
				if (mul.target.reg == REGISTER.FP) mul.target.reg = REGISTER.SP;
				if (mul.op0.reg == REGISTER.FP) mul.op0.reg = REGISTER.SP;
				if (mul.op1.reg == REGISTER.FP) mul.op1.reg = REGISTER.SP;
			}
			else if (ins instanceof ASMStrStack) {
				ASMStrStack str = (ASMStrStack) ins;
				if (str.target.reg == REGISTER.FP) str.target.reg = REGISTER.SP;
				
				if (str.op1 instanceof PatchableImmOperand) {
					PatchableImmOperand op = (PatchableImmOperand) str.op1;
					int words = this.countPushedWords(patch, str);
					str.op1 = new ImmOperand(op.patchedValue + (words * 4));
				}
				else if (str.op1 instanceof ImmOperand && str.op0 instanceof RegOperand && ((RegOperand) str.op0).reg == REGISTER.FP) {
					ImmOperand op = (ImmOperand) str.op1;
					int words = this.countPushedWords(patch, str);
					str.op1 = new ImmOperand(op.value + (words * 4));
				}
				
				if (str.op0 instanceof RegOperand) {
					RegOperand op = (RegOperand) str.op0;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
				if (str.op1 instanceof RegOperand) {
					RegOperand op = (RegOperand) str.op1;
					if (op.reg == REGISTER.FP) op.reg = REGISTER.SP;
				}
			}
			else if (ins instanceof ASMPushStack) {
				ASMPushStack push = (ASMPushStack) ins;
				for (RegOperand r : push.operands) if (r.reg == REGISTER.FP) r.reg = REGISTER.SP;
			}
			else if (ins instanceof ASMPopStack) {
				ASMPopStack pop = (ASMPopStack) ins;
				for (RegOperand r : pop.operands) if (r.reg == REGISTER.FP) r.reg = REGISTER.SP;
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
				
				if (mov.optFlags.contains(OPT_FLAG.WRITEBACK) || mov.target.reg == REGISTER.PC) continue;
				
				if (mov.op1 instanceof RegOperand) {
					REGISTER reg = ((RegOperand) mov.op1).reg;
				
					if (RegOperand.toInt(reg) > 9 || RegOperand.toInt(mov.target.reg) > 9) continue;

					boolean remove = true;
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						ASMInstruction ins = body.instructions.get(a);
						
						if (ins instanceof ASMBranch || ins instanceof ASMLabel || (ins instanceof ASMMov && ((ASMMov) ins).target.reg == REGISTER.PC)) {
							remove = false;
							break;
						}
						
						if (ins instanceof ASMBinaryData) {
							ASMBinaryData d = (ASMBinaryData) ins;
							
							if (d.op0 != null && d.op0.reg == mov.target.reg && d.op0.reg != reg) {
								d.op0.reg = reg;
								markOpt();
							}
							if (d.op1 instanceof RegOperand) {
								RegOperand op1 = (RegOperand) d.op1;
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
							if (d.op1 instanceof RegOperand) {
								RegOperand op1 = (RegOperand) d.op1;
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
							if (d.op0 instanceof RegOperand) {
								RegOperand op = (RegOperand) d.op0;
								if (op.reg == mov.target.reg && op.reg != reg) {
									op.reg = reg;
									markOpt();
								}
							}
							if (d.op1 instanceof RegOperand) {
								RegOperand op1 = (RegOperand) d.op1;
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
				
				if (mov.target.reg == REGISTER.FP && mov.op1 instanceof RegOperand && ((RegOperand) mov.op1).reg == REGISTER.SP) {
					boolean clear = true;
					ASMMov mov0 = null;
					
					List<ASMInstruction> patch = new ArrayList();
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMMov) {
							mov0 = (ASMMov) body.instructions.get(a);
							if (mov0.target.reg == REGISTER.SP && mov0.op1 instanceof RegOperand && ((RegOperand) mov0.op1).reg == REGISTER.FP) {
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
						if (push == null || push.operands.size() == 1 && push.operands.get(0).reg == REGISTER.FP) {
							
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
								
								if (push != null) this.patchFramePointerAddressing(patch, 4);
								
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
				
				RegOperand lr = null;
				for (RegOperand r : push.operands) if (r.reg == REGISTER.LR) {
					lr = r;
					break;
				}
				
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
						else if (body.instructions.get(a) instanceof ASMMov && ((ASMMov) body.instructions.get(a)).target.reg == REGISTER.PC) {
							clear = false;
						}
						else if (branch != null) {
							clear = false;
							break;
						}
					}
					
					if (clear) {
						push.operands.remove(lr);
						for (RegOperand r : pop.operands) {
							if (r.reg == REGISTER.LR) {
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
					
					if (pop.operands.size() > 0 && pop.operands.get(pop.operands.size() - 1).reg == REGISTER.LR) {
						pop.operands.set(pop.operands.size() - 1, new RegOperand(REGISTER.PC));
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
				
				if (stackOp.op0 != null && stackOp.op0.reg == REGISTER.FP) {
					if (stackOp.op1 instanceof ImmOperand && ((ImmOperand) stackOp.op1).patchable != null) {
						PatchableImmOperand op = ((ImmOperand) stackOp.op1).patchable;
						
						/* Patch the offset for parameters because they are located under the pushed regs,
						 * dont patch local data since its located above the pushed regs.
						 */
						if (op.dir == PATCH_DIR.UP) {
							((ImmOperand) stackOp.op1).value -= sub;
						}
					}
				}
			}
			else if (ins instanceof ASMBinaryData) {
				ASMBinaryData binary = (ASMBinaryData) ins;
				
				if (binary.op0 != null && binary.op0.reg == REGISTER.FP) {
					if (binary.op1 instanceof ImmOperand && ((ImmOperand) binary.op1).patchable != null) {
						PatchableImmOperand op = ((ImmOperand) binary.op1).patchable;
						
						if (op.dir == PATCH_DIR.UP) {
							((ImmOperand) binary.op1).value -= sub;
						}
					}
				}
			}
			else if (ins instanceof ASMMemOp) {
				ASMMemOp mem = (ASMMemOp) ins;
				
				if (mem.op0 != null && mem.op0 instanceof RegOperand && ((RegOperand) mem.op0).reg == REGISTER.FP) {
					if (mem.op1 instanceof ImmOperand && ((ImmOperand) mem.op1).patchable != null) {
						PatchableImmOperand op = ((ImmOperand) mem.op1).patchable;
						
						if (op.dir == PATCH_DIR.UP) {
							((ImmOperand) mem.op1).value -= sub;
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
						RegOperand reg = push.operands.get(x);
						
						/* Only for regular registers */
						if (RegOperand.toInt(reg.reg) < 10) {
							
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
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand) {
					LabelOperand label = (LabelOperand) branch.target;
				
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
									if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand && ((LabelOperand) branch.target).label.equals(label.label)) {
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
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand) {
					LabelOperand label = (LabelOperand) branch.target;
				
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
							if (pop.operands.get(pop.operands.size() - 1).reg == REGISTER.PC) {
								for (int a = i; a < x + 1; a++) {
									if (body.instructions.get(a) instanceof ASMBranch) {
										branch = (ASMBranch) body.instructions.get(a);
										if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand && ((LabelOperand) branch.target).label.equals(label.label)) {
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
				
				REGISTER mReg0 = mul.op0.reg;
				REGISTER mReg1 = mul.op1.reg;
				
				ImmOperand op0 = null;
				ImmOperand op1 = null;
				
				boolean last = false;
				
				if (body.instructions.get(i - 2) instanceof ASMMov) {
					ASMMov mov = (ASMMov) body.instructions.get(i - 2);
					if (RegOperand.toInt(mov.target.reg) < 3 && mov.op1 instanceof ImmOperand) {
						if (mov.target.reg == mReg0) op0 = (ImmOperand) mov.op1;
						if (mov.target.reg == mReg1) op1 = (ImmOperand) mov.op1;
					}
				}
				
				if (body.instructions.get(i - 1) instanceof ASMMov) {
					ASMMov mov = (ASMMov) body.instructions.get(i - 1);
					if (RegOperand.toInt(mov.target.reg) < 3 && mov.op1 instanceof ImmOperand) {
						if (mov.target.reg == mReg0) op0 = (ImmOperand) mov.op1;
						if (mov.target.reg == mReg1) {
							op1 = (ImmOperand) mov.op1;
							last = true;
						}
					}
				}
				
				if (op0 != null && op1 != null) {
					int r = op0.value * op1.value;
					
					/* Can only move a value 0 <= r <= 255 */
					if (r <= 255) {
						body.instructions.set(i, new ASMMov(new RegOperand(mul.target.reg), new ImmOperand(r)));
						
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
						body.instructions.set(i, new ASMLsl(mul.target, mul.op0, new ImmOperand(log)));
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
					
					if (mul.target.reg == add.op0.reg && add.op1 instanceof RegOperand) {
						ASMMla mla = new ASMMla(add.target, mul.op0, mul.op1, (RegOperand) add.op1);
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
				
				if (ldr.op1 != null || !(ldr.op0 instanceof RegOperand)) continue;
				
				REGISTER dataTarget = null;
				RegOperand op0 = null;
				Operand op1 = null;
				
				Shift shift = null;
				
				boolean negate = false;
				
				if (body.instructions.get(i - 1) instanceof ASMAdd) {
					ASMAdd add = (ASMAdd) body.instructions.get(i - 1);
					
					dataTarget = add.target.reg;
					op0 = add.op0;
					op1 = add.op1;
				}
				else if (body.instructions.get(i - 1) instanceof ASMSub) {
					ASMSub sub = (ASMSub) body.instructions.get(i - 1);
					
					dataTarget = sub.target.reg;
					op0 = sub.op0;
					
					if (sub.op1 instanceof RegOperand) {
						op1 = sub.op1;
						negate = true;
					}
					else if (sub.op1 instanceof ImmOperand) {
						ImmOperand imm = (ImmOperand) sub.op1;
						op1 = new ImmOperand(-imm.value);
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
					op0 = lsl.op0.clone();
					
					op1 = lsl.op0.clone();
					shift = new Shift(SHIFT.LSL, lsl.op1.clone());
				}
				else continue;
				
				REGISTER ldrTarget = ((RegOperand) ldr.op0).reg;
					
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
					if (shift == null) {
						/* Substitute */
						ldr.op0 = op0;
						ldr.op1 = op1;
					}
					else {
						/* Special treatment for shifts */
						
						/* Is always 0 */
						ldr.op0 = new RegOperand(REGISTER.R10);
						
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
				List<REGISTER> probed = new ArrayList();
				
				for (int k = i; k < body.instructions.size(); k++) {
					if (body.instructions.get(k) instanceof ASMMov) {
						ASMMov mov = (ASMMov) body.instructions.get(k);
						REGISTER reg = mov.target.reg;
						
						/*
						 *  Only probe first occurrence of assignment to register after 
						 *  function start. If mov instruction would not be first, but last
						 *  to assign to register, algorithm would delete the instruction
						 *  destroying the data flow.
						 */
						if (probed.contains(reg)) continue;
						else probed.add(reg);
						
						if (RegOperand.toInt(reg) > 2 && reg != REGISTER.R10 && reg != REGISTER.FP && reg != REGISTER.SP && reg != REGISTER.LR && reg != REGISTER.PC && reg != REGISTER.R12) {
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
			REGISTER reg = null;
			
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
			
			if (RegOperand.toInt(reg) < 3) {
				for (int a = i + 1; a < body.instructions.size(); a++) {
					ASMInstruction ins = body.instructions.get(a);
					
					if (readsReg(ins, reg) || ins instanceof ASMBranch || overwritesReg(ins, REGISTER.PC)) {
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
			else if (RegOperand.toInt(reg) < 10) {
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
					
					REGISTER reg = push.operands.get(0).reg;
					
					boolean afterFPExchange = false;
					int line = i;
					while (true) {
						if (overwritesReg(body.instructions.get(line), reg) || 
								body.instructions.get(line) instanceof ASMBranch || body.instructions.get(line) instanceof ASMLabel || 
								body.instructions.get(line) instanceof ASMStackOp || body.instructions.get(line) instanceof ASMPushStack ||
								body.instructions.get(line) instanceof ASMLdr || body.instructions.get(line) instanceof ASMPopStack) {
							break;
						}
						else if (readsReg(body.instructions.get(line), REGISTER.SP)) break;
						else {
							if (body.instructions.get(line) instanceof ASMMov) {
								ASMMov mov = (ASMMov) body.instructions.get(line);
								if (mov.target.reg == REGISTER.SP && mov.op1 instanceof RegOperand && ((RegOperand) mov.op1).reg == REGISTER.FP) {
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
					
					REGISTER reg = pop.operands.get(0).reg;
					
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
						add0.op1 instanceof ImmOperand && add1.op1 instanceof ImmOperand) {
					ImmOperand op0 = (ImmOperand) add0.op1;
					ImmOperand op1 = (ImmOperand) add1.op1;
					
					op0.value += op1.value;
					body.instructions.remove(i);
					i--;
					markOpt();
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
						sub.op1 instanceof ImmOperand && add.op1 instanceof ImmOperand) {
					ImmOperand subOp = (ImmOperand) sub.op1;
					ImmOperand addOp = (ImmOperand) add.op1;
					
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
				
				if (mov.target.reg == add.op0.reg && mov.op1 instanceof ImmOperand && add.op1 instanceof RegOperand) {
					RegOperand op1 = (RegOperand) add.op1;
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
				
				if (lsl.target.reg == add.op0.reg && lsl.op1 instanceof ImmOperand && add.op1 instanceof RegOperand) {
					RegOperand op1 = (RegOperand) add.op1;
					add.op1 = lsl.op0;
					
					add.shiftType = SHIFT_TYPE.LSL;
					add.shiftDist = ((ImmOperand) lsl.op1).value;
					
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
				
				if (mov.target.reg == sub.op0.reg && mov.op1 instanceof ImmOperand && sub.op1 instanceof RegOperand) {
					RegOperand op1 = (RegOperand) sub.op1;
					
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
				if (add.target.reg == add.op0.reg && add.op1 instanceof ImmOperand && !add.updateConditionField) {
					ImmOperand imm = (ImmOperand) add.op1;
					if (imm.value == 0) {
						body.instructions.remove(i);
						i--;
						markOpt();
					}
				}
			}
			else if (body.instructions.get(i) instanceof ASMSub) {
				ASMSub sub = (ASMSub) body.instructions.get(i);
				if (sub.target.reg == sub.op0.reg && sub.op1 instanceof ImmOperand && !sub.updateConditionField) {
					ImmOperand imm = (ImmOperand) sub.op1;
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
					REGISTER reg = ((RegOperand) push.operands.get(0)).reg;
					for (int a = i; a < body.instructions.size(); a++) {
						ASMInstruction ins = body.instructions.get(a);
						
						if (ins instanceof ASMPopStack) {
							ASMPopStack pop = (ASMPopStack) ins;
							
							if (pop.operands.size() == 1 && !pop.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
								RegOperand op0 = (RegOperand) pop.operands.get(0);
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
					REGISTER pushReg = push.operands.get(0).reg;
					
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
						REGISTER newReg = pop.operands.get(0).reg;
						
						/* Check if register if newReg is overwritten in the span between the push pop */
						for (int a = i + 1; a < end; a++) {
							/* Old register is overwritten, value from mov would be shadowed */
							if (overwritesReg(body.instructions.get(a), push.operands.get(0).reg)) {
								replace = false;
								break;
							}
						}
						
						if (replace) {
							body.instructions.set(body.instructions.indexOf(pop), new ASMMov(new RegOperand(newReg), new RegOperand(pushReg)));
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
				
				if (mov.op1 instanceof RegOperand) {
					REGISTER reg = ((RegOperand) mov.op1).reg;
					if (reg == REGISTER.R0 || reg == REGISTER.R1 || reg == REGISTER.R2) {
						
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
				
				if (mov.op1 instanceof RegOperand) {
					REGISTER reg = ((RegOperand) mov.op1).reg;
					
					/* Only perform action if target is a operand register. */
					if (RegOperand.toInt(reg) > 2) continue;
					
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
				
				if (mov.op1 instanceof RegOperand) {
					REGISTER reg = ((RegOperand) mov.op1).reg;
					
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
					if (mov.target.reg == RegOperand.toReg(a) && mov.op1 instanceof RegOperand) {
						if (body.instructions.get(i) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) body.instructions.get(i);
							if (cmp.op0 != null && cmp.op0.reg == RegOperand.toReg(a)) {
								/* Replace */
								cmp.op0 = (RegOperand) mov.op1;
								markOpt();
								
								body.instructions.remove(i - 1);
								i--;
							}
							else if (cmp.op1 != null && cmp.op1 instanceof RegOperand && ((RegOperand) cmp.op1).reg == RegOperand.toReg(a)) {
								/* Replace */
								cmp.op1 = (RegOperand) mov.op1;
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
				
				if (str.target.reg == mov.target.reg && mov.op1 instanceof RegOperand) {
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
						str.target = (RegOperand) mov.op1;
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
					if (mov.target.reg == RegOperand.toReg(a) && mov.op1 instanceof RegOperand) {
						RegOperand target = (RegOperand) mov.op1;
						
						/* Writeback flag is set, cannot substitute, since mov copies operand */
						if (body.instructions.get(i).optFlags.contains(OPT_FLAG.WRITEBACK)) {
							break;
						}
						
						if (body.instructions.get(i) instanceof ASMBinaryData && !(body.instructions.get(i) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) body.instructions.get(i);
							boolean remove = false;
							if (data.op0 != null && data.op0.reg == RegOperand.toReg(a)) {
								/* Replace */
								data.op0 = target;
								markOpt();
								remove = true;
							}
							
							if (data.op1 != null && data.op1 instanceof RegOperand && ((RegOperand) data.op1).reg == RegOperand.toReg(a)) {
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
							if (mul.op0 != null && mul.op0.reg == RegOperand.toReg(a)) {
								/* Replace */
								mul.op0 = target;
								markOpt();
								remove = true;
							}
							
							if (mul.op1 != null && mul.op1 instanceof RegOperand && ((RegOperand) mul.op1).reg == RegOperand.toReg(a)) {
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
								
								if (mul.op0 != null && mul.op0.reg == RegOperand.toReg(a)) {
									/* Replace */
									mul.op0 = target;
									markOpt();
									remove = true;
								}
								
								if (mul.op1 != null && mul.op1 instanceof RegOperand && mul.op1.reg == RegOperand.toReg(a)) {
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
				
				if (mov.target.reg == REGISTER.R0 && mov.op1 instanceof RegOperand) {
					if (body.instructions.get(i) instanceof ASMPushStack) {
						ASMPushStack push = (ASMPushStack) body.instructions.get(i);
						
						if (push.operands.size() == 1 && push.operands.get(0).reg == mov.target.reg) {
							push.operands.get(0).reg = ((RegOperand) mov.op1).reg;
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
				
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand) {
					LabelOperand op = (LabelOperand) branch.target;
					
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
				if (move0.op1 instanceof RegOperand && move1.op1 instanceof RegOperand) {
					RegOperand op0 = (RegOperand) move0.op1;
					RegOperand op1 = (RegOperand) move1.op1;
					if (op0.reg == REGISTER.FP || op1.reg == REGISTER.FP) continue;
					
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
				if (mov.op1 instanceof RegOperand && ((RegOperand) mov.op1).reg == mov.target.reg) {
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
				if (mov.op1 instanceof RegOperand && ((RegOperand) mov.op1).reg == mov.target.reg) {
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
				
				if (move.op1 instanceof ImmOperand) {
					int val = ((ImmOperand) move.op1).value;
					REGISTER target = move.target.reg;
					
					/* Set to false if reg is read */
					boolean clear = true;
					
					boolean hardClear = false;
					
					/* Break is reg is overwritten */
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMBranch ||
							body.instructions.get(a) instanceof ASMLabel ||
							body.instructions.get(a) instanceof ASMMov && ((ASMMov) body.instructions.get(a)).target.reg == REGISTER.PC) {
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
							
							if (move0.op1 instanceof RegOperand && ((RegOperand) move0.op1).reg == target) {
								move0.op1 = new ImmOperand(val);
								markOpt();
							}
						}
						else if (body.instructions.get(a) instanceof ASMMvn) {
							ASMMvn move0 = (ASMMvn) body.instructions.get(a);
							
							if (move0.target.reg == target) {
								if (move0.cond != null) clear = false;
								break;
							}
							
							if (move0.op1 instanceof RegOperand && ((RegOperand) move0.op1).reg == target) {
								move0.op1 = new ImmOperand(val);
								markOpt();
							}
						}
						else if (body.instructions.get(a) instanceof ASMBinaryData) {
							ASMBinaryData dataP = (ASMBinaryData) body.instructions.get(a);
							if (dataP.op1 instanceof RegOperand && ((RegOperand) dataP.op1).reg == target) {
								dataP.op1 = new ImmOperand(val);
								markOpt();
								
								if (!move.optFlags.contains(OPT_FLAG.WRITEBACK)) hardClear = true;
							}
							
							if (dataP.op0 instanceof RegOperand && ((RegOperand) dataP.op0).reg == target) {
								/* mov r1, #10; add r0, r1, #5 -> mov r0, #15 */
								if (dataP.op1 instanceof ImmOperand) {
									int value = ((ImmOperand) dataP.op1).value;
									ASMInstruction ins = null;
									
									int result = dataP.solver.solve(val, value);
									if (result >= 0) {
										ins = new ASMMov(dataP.target, new ImmOperand(result));
									}
									else {
										/* Invert in twos complement */
										int inv = -(result + 1);
										ins = new ASMMvn(dataP.target, new ImmOperand(inv));
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
							
							if (cmp.op1 instanceof RegOperand && ((RegOperand) cmp.op1).reg == target) {
								cmp.op1 = new ImmOperand(val);
								markOpt();
							}
							
							if (cmp.op0.reg == target) {
								clear = false;
							}
						}
						else if (body.instructions.get(a) instanceof ASMPushStack) {
							ASMPushStack p = (ASMPushStack) body.instructions.get(a);
							for (RegOperand r : p.operands) {
								if (r.reg == target) {
									clear = false;
									break;
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMPopStack) {
							ASMPopStack p = (ASMPopStack) body.instructions.get(a);
							boolean end = false;
							for (RegOperand r : p.operands) {
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
							
							if (p.op1 instanceof RegOperand) {
								RegOperand r = (RegOperand) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOperand(val);
									markOpt();
								}
							}
						}
						else if (body.instructions.get(a) instanceof ASMLdr) {
							ASMLdr p = (ASMLdr) body.instructions.get(a);
							if (p.target.reg == target) {
								break;
							}
							
							if (p.op0 instanceof RegOperand && ((RegOperand) p.op0).reg == target) {
								clear = false;
							}
							
							if (p.op1 instanceof RegOperand) {
								RegOperand r = (RegOperand) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOperand(val);
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
							
							if (p.op0 instanceof RegOperand && ((RegOperand) p.op0).reg == target) {
								clear = false;
							}
							
							if (p.op1 instanceof RegOperand) {
								RegOperand r = (RegOperand) p.op1;
								if (r.reg == target) {
									p.op1 = new ImmOperand(val);
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
							
							if (p.op0.reg == target || (p.op1 instanceof RegOperand && ((RegOperand) p.op1).reg == target)) {
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
							new Message("ASMOPT -> ConstOp propagation : Not available " + body.instructions.get(a).getClass().getName(), Type.WARN);
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
				if (b.target instanceof LabelOperand) usedLabels.add(((LabelOperand) b.target).label);
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
