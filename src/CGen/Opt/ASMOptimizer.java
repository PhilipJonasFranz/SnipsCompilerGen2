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
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AsN.AsNBody;
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

	
			/* --- METHODS --- */
	/** Optimize given body. */
	public void optimize(AsNBody body) {
		
		/* While an optimization was done in this iteration */
		while (OPT_DONE) {
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
			this.removeLoadIndirectTargeting(body);
			
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
			
			this.multPrecalc(body);

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
		}
		
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
		else if (ins instanceof ASMMemOp) {
			ASMMemOp memOp = (ASMMemOp) ins;
			return memOp.target.reg == reg;
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
	
	private void multPrecalc(AsNBody body) {
		for (int i = 2; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMult) {
				ASMMult mul = (ASMMult) body.instructions.get(i);
				
				REGISTER mReg0 = mul.op0.reg;
				REGISTER mReg1 = mul.op1.reg;
				
				ImmOperand op0 = null;
				ImmOperand op1 = null;
				
				if (RegOperand.toInt(mReg0) < 3 && RegOperand.toInt(mReg1) < 3) {
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
							if (mov.target.reg == mReg1) op1 = (ImmOperand) mov.op1;
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
							
							OPT_DONE = true;
						}
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
				
				REGISTER target = null;
				RegOperand op0 = null;
				Operand op1 = null;
				
				Shift shift = null;
				
				boolean negate = false;
				
				if (body.instructions.get(i - 1) instanceof ASMAdd) {
					ASMAdd add = (ASMAdd) body.instructions.get(i - 1);
					
					target = add.target.reg;
					op0 = add.op0;
					op1 = add.op1;
				}
				else if (body.instructions.get(i - 1) instanceof ASMSub) {
					ASMSub sub = (ASMSub) body.instructions.get(i - 1);
					
					target = sub.target.reg;
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
					
					target = lsl.target.reg;
					op0 = lsl.op0.clone();
					
					op1 = lsl.op0.clone();
					shift = new Shift(SHIFT.LSL, lsl.op1.clone());
				}
				else continue;
				
				REGISTER addr = ((RegOperand) ldr.op0).reg;
					
				if (target != addr) continue;
				
				boolean clear = true;
				
				for (int a = i + 1; a < body.instructions.size(); a++) {
					if (readsReg(body.instructions.get(a), target)) {
						clear = false;
						break;
					}
					if (overwritesReg(body.instructions.get(a), target) && 
						!readsReg(body.instructions.get(a), target)) {
						break;
					}
				}
				
				/* Ldr itself will overwrite register */
				clear |= ldr.target.reg == target;
				
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
					OPT_DONE = true;
				}
			}
		}
	}
	
	private void removeUnusedRegistersStrict(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				REGISTER reg = mov.target.reg;
				
				if (RegOperand.toInt(reg) > 2 && reg != REGISTER.R10 && reg != REGISTER.FP && reg != REGISTER.SP && reg != REGISTER.LR && reg != REGISTER.PC) {
					boolean used = false;
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMBranch && ((ASMBranch) body.instructions.get(a)).type == BRANCH_TYPE.BX) break;
						else {
							used |= readsReg(body.instructions.get(a), reg);
						}
					}
					
					if (!used) {
						body.instructions.remove(i);
						i--;
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
						OPT_DONE = true;
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
				
				if (push.operands.size() == 1 && !push.optFlags.contains(OPT_FLAG.FUNC_CLEAN)) {
					
					body.instructions.remove(i);
					
					REGISTER reg = push.operands.get(0).reg;
					
					int line = i;
					while (true) {
						if (overwritesReg(body.instructions.get(line), reg) || 
								body.instructions.get(line) instanceof ASMBranch || body.instructions.get(line) instanceof ASMLabel || 
								body.instructions.get(line) instanceof ASMStackOp || body.instructions.get(line) instanceof ASMPushStack ||
								body.instructions.get(line) instanceof ASMLdr || body.instructions.get(line) instanceof ASMPopStack) {
							break;
						}
						else line++;
					}
					
					if (line != i) OPT_DONE = true;
					body.instructions.add(line, push);
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
					
					if (line != i - 1) OPT_DONE = true;
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
					OPT_DONE = true;
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
						OPT_DONE = true;
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
					OPT_DONE = true;
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
					OPT_DONE = true;
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
						OPT_DONE = true;
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
						OPT_DONE = true;
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
							
							/* New register is overwritten, value from mov would be shadowed */
							if (overwritesReg(body.instructions.get(a), newReg)) {
								replace = false;
								break;
							}
							
							/* New register is read after new mov, would read wrong value */
							if (readsReg(body.instructions.get(a), newReg)) {
								replace = false;
								break;
							}
						}
						
						if (replace) {
							body.instructions.set(i, new ASMMov(new RegOperand(newReg), new RegOperand(pushReg)));
							body.instructions.remove(pop);
							
							OPT_DONE = true;
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
								OPT_DONE = true;
								patchUp = true;
							}
						}
						else if (body.instructions.get(i - 1) instanceof ASMMult) {
							ASMMult mul = (ASMMult) body.instructions.get(i - 1);
							if (mul.target.reg == reg && replace) {
								mul.target = mov.target;
								OPT_DONE = true;
								patchUp = true;
							}
						}
						
						if (patchUp) body.instructions.remove(i);
					}
				}
			}
		}
	}
	
	private void removeLoadIndirectTargeting(AsNBody body) {
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
							OPT_DONE = true;
							body.instructions.remove(i);
							i--;
						}
					}
					else if (body.instructions.get(i - 1) instanceof ASMLdr) {
						ASMLdr ldr = (ASMLdr) body.instructions.get(i - 1);
						
						if (ldr.target.reg == reg) {
							ldr.target.reg = mov.target.reg;
							OPT_DONE = true;
							body.instructions.remove(i);
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
								OPT_DONE = true;
								
								body.instructions.remove(i - 1);
								i--;
							}
							else if (cmp.op1 != null && cmp.op1 instanceof RegOperand && ((RegOperand) cmp.op1).reg == RegOperand.toReg(a)) {
								/* Replace */
								cmp.op1 = (RegOperand) mov.op1;
								OPT_DONE = true;
								
								body.instructions.remove(i - 1);
								i--;
							}
						}
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
								OPT_DONE = true;
								remove = true;
							}
							
							if (data.op1 != null && data.op1 instanceof RegOperand && ((RegOperand) data.op1).reg == RegOperand.toReg(a)) {
								/* Replace */
								data.op1 = target;
								OPT_DONE = true;
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
								OPT_DONE = true;
								remove = true;
							}
							
							if (mul.op1 != null && mul.op1 instanceof RegOperand && ((RegOperand) mul.op1).reg == RegOperand.toReg(a)) {
								/* Replace */
								mul.op1 = target;
								OPT_DONE = true;
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
								if (mul.op0 != null && mul.op0.reg == RegOperand.toReg(a)) {
									/* Replace */
									mul.op0 = target;
									OPT_DONE = true;
									remove = true;
								}
								
								if (mul.op1 != null && mul.op1 instanceof RegOperand && ((RegOperand) mul.op1).reg == RegOperand.toReg(a)) {
									/* Replace */
									mul.op1 = target;
									OPT_DONE = true;
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
							OPT_DONE = true;
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
						OPT_DONE = true;
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
						OPT_DONE = true;
					}
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
								OPT_DONE = true;
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
								OPT_DONE = true;
							}
						}
						else if (body.instructions.get(a) instanceof ASMBinaryData) {
							ASMBinaryData dataP = (ASMBinaryData) body.instructions.get(a);
							if (dataP.op1 instanceof RegOperand && ((RegOperand) dataP.op1).reg == target) {
								dataP.op1 = new ImmOperand(val);
								OPT_DONE = true;
								
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
									OPT_DONE = true;
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
						else if (body.instructions.get(a) instanceof ASMCmp) {
							ASMCmp cmp = (ASMCmp) body.instructions.get(a);
							
							if (cmp.op1 instanceof RegOperand && ((RegOperand) cmp.op1).reg == target) {
								cmp.op1 = new ImmOperand(val);
								OPT_DONE = true;
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
									OPT_DONE = true;
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
									OPT_DONE = true;
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
									OPT_DONE = true;
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
						else {
							clear = false;
							new Message("ASMOPT -> ConstOp propagation : Not available " + body.instructions.get(a).getClass().getName(), Type.WARN);
						}
						
					}
					
					if (clear || hardClear) {
						body.instructions.remove(i);
						i--;
						OPT_DONE = true;
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
					OPT_DONE = true;
					while (i < body.instructions.size() && !(ins instanceof ASMLabel)) {
						body.instructions.remove(i);
					}
					i--;
				}
			}
		}
	}
	
	private void clearInstructionsAfterBranch(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
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
						OPT_DONE = true;
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
					OPT_DONE = true;
				}
			}
		}
	}
	
}
