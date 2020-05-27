package CGen.Opt;

import java.util.ArrayList;
import java.util.List;

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
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.ASMBinaryData;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMMvn;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
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
			 * add r0, r1, r2<br>
			 * mov r4, r0<br>
			 * Replace with:<br>
			 * add r4, r1, r2
			 */
			this.removeExpressionIndirectTargeting(body);
			
			/**
			 * mov r0, rx<br>
			 * cmp rx, r1<br>
			 * Replace with:<br>
			 * cmp rx, r1
			 */
			this.removeOperandIndirectTargeting(body);
			
			this.constantOperandPropagation(body);
			
			/**
			 * b .L1<br>
			 * ... <- Remove these Lines until Label or newline
			 */
			this.clearInstructionsAfterBranch(body);
			
			/**
			 * Removes non-referenced Labels
			 */
			this.clearUnusedLabels(body);
			
			/**
			 * mov r1, r0<br>
			 * mov r0, r1 <-- Delete this line
			 */
			this.removeDoubleCrossing(body);
			
			/**
			 * push { r0 }<br>
			 * pop { r1 }<br>
			 * Replace with:<br>
			 * mov r1, r0
			 */
			this.removeImplicitStackAssignment(body);
			
			/**
			 * b .Lx <-- Remove this line<br>
			 * .Lx: 
			 */
			this.removeBranchesBeforeLabelToLabel(body);
			
			/**
			 * push { r0 }<br>
			 * ... <- Does not reassign r0<br>
			 * pop { r0 }
			 */
			this.removeUnnessesaryPushPop(body);
		}
	}
	
	/**
	 * Check if given register is overwritten by given instruction.
	 */
	protected boolean overwritesReg(ASMInstruction ins, REGISTER reg) {
		if (ins instanceof ASMBinaryData) {
			ASMBinaryData data = (ASMBinaryData) ins;
			return data.target.reg == reg;
		}
		else if (ins instanceof ASMMemOp) {
			ASMMemOp memOp = (ASMMemOp) ins;
			return memOp.target.reg == reg;
		}
		else if (ins instanceof ASMLdrStack) {
			ASMLdrStack load = (ASMLdrStack) ins;
			return load.target.reg == reg;
		}
		else if (ins instanceof ASMPopStack) {
			ASMPopStack pop = (ASMPopStack) ins;
			return pop.operands.stream().filter(x -> x.reg == reg).count() > 0;
		}
		else return false;
	}
	
	protected void removeUnnessesaryPushPop(AsNBody body) {
		
		
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
						
						if (this.overwritesReg(ins, reg) || ins instanceof ASMBranch || ins instanceof ASMLabel) {
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
	
	protected void removeExpressionIndirectTargeting(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov mov = (ASMMov) body.instructions.get(i);
				
				if (mov.op1 instanceof RegOperand) {
					REGISTER reg = ((RegOperand) mov.op1).reg;
					if (reg == REGISTER.R0 || reg == REGISTER.R1 || reg == REGISTER.R2) {
						if (body.instructions.get(i - 1) instanceof ASMBinaryData && !(body.instructions.get(i - 1) instanceof ASMMov)) {
							ASMBinaryData data = (ASMBinaryData) body.instructions.get(i - 1);
							if (data.target.reg == reg) {
								boolean replace = true;
								
								if (this.overwritesReg(body.instructions.get(i - 2), reg)) {
									replace = false;
								}
								
								if (body.instructions.get(i + 1) instanceof ASMCmp) {
									ASMCmp cmp = (ASMCmp) body.instructions.get(i + 1);
									if (cmp.op0.reg == reg) replace = false;
								}
								
								if (replace) {
									/* Replace */
									data.target = mov.target;
									OPT_DONE = true;
									
									if (!this.overwritesReg(body.instructions.get(i - 2), reg)) {
										body.instructions.remove(i);
										i--;
									}
									else {
										ASMInstruction rem = body.instructions.remove(i);
										body.instructions.add(i - 1, rem);
										i--;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected void removeOperandIndirectTargeting(AsNBody body) {
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
				for (int a = 0; a < 15; a++) {
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
					}
				}
			}
		}
	}
	
	protected void removeBranchesBeforeLabelToLabel(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMBranch && body.instructions.get(i) instanceof ASMLabel) {
				ASMBranch branch = (ASMBranch) body.instructions.get(i - 1);
				ASMLabel label = (ASMLabel) body.instructions.get(i);
				if (branch.type == BRANCH_TYPE.B && branch.target instanceof LabelOperand) {
					LabelOperand op = (LabelOperand) branch.target;
					if (op.label.equals(label)) {
						body.instructions.remove(i - 1);
						i--;
						OPT_DONE = true;
					}
				}
			}
		}
	}
	
	protected void removeImplicitStackAssignment(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMPushStack && body.instructions.get(i) instanceof ASMPopStack) {
				ASMPushStack push = (ASMPushStack) body.instructions.get(i - 1);
				ASMPopStack pop = (ASMPopStack) body.instructions.get(i);
				if (push.operands.size() == 1 && pop.operands.size() == 1) {
					body.instructions.remove(i - 1);
					body.instructions.remove(i - 1);
					body.instructions.add(i - 1, new ASMMov(pop.operands.get(0), push.operands.get(0)));
					OPT_DONE = true;
				}
			}
		}
	}
	
	protected void removeDoubleCrossing(AsNBody body) {
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
	
	protected void constantOperandPropagation(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov move = (ASMMov) body.instructions.get(i);
				
				if (move.cond != null) continue;
				
				if (move.op1 instanceof ImmOperand) {
					int val = ((ImmOperand) move.op1).value;
					REGISTER target = move.target.reg;
					
					/* Set to false if reg is read */
					boolean clear = true;
					
					/* Break is reg is overwritten */
					
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMBranch ||
							body.instructions.get(a) instanceof ASMLabel) {
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
						else {
							clear = false;
							new Message("ASMOPT -> ConstOp propagation : Not available " + body.instructions.get(a).getClass().getName(), Type.WARN);
						}
						
					}
					
					if (clear) {
						body.instructions.remove(i);
						i--;
						OPT_DONE = true;
					}
				}
			}
		}
	}
	
	protected void clearUnusedLabels(AsNBody body) {
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
	
	protected void clearInstructionsAfterBranch(AsNBody body) {
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
	
}
