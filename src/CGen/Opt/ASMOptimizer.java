package CGen.Opt;

import java.util.ArrayList;
import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.ASMCmp;
import Imm.ASM.Processing.ASMDataP;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Processing.ASMMult;
import Imm.ASM.Processing.ASMMvn;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AsN.AsNBody;
import Util.Logging.Message;
import Util.Logging.Message.Type;

public class ASMOptimizer {

	public ASMOptimizer() {
		
	}
	
	boolean OPT_DONE = true;
	
	public void optimize(AsNBody body) {
	
		while (OPT_DONE) {
			OPT_DONE = false;
			
			this.constantOperandPropagation(body);
			
			this.clearInstructionsAfterBranch(body);
			
			this.clearUnusedLabels(body);
			
			this.removeDoubleCrossing(body);
			
			this.removeImplicitStackAssignment(body);
		}
		
	}
	
	/**
	 * push { r0 }
	 * pop { r1 }
	 * Replace with:
	 * mov r1, r0
	 */
	public void removeImplicitStackAssignment(AsNBody body) {
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
	
	/**
	 * mov r1, r0
	 * mov r0, r1 <-- Delete this line
	 */
	public void removeDoubleCrossing(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMov && body.instructions.get(i) instanceof ASMMov) {
				ASMMov move0 = (ASMMov) body.instructions.get(i - 1);
				ASMMov move1 = (ASMMov) body.instructions.get(i);
				if (move0.origin instanceof RegOperand && move1.origin instanceof RegOperand) {
					RegOperand op0 = (RegOperand) move0.origin;
					RegOperand op1 = (RegOperand) move1.origin;
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
	
	public void constantOperandPropagation(AsNBody body) {
		for (int i = 0; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMMov) {
				ASMMov move = (ASMMov) body.instructions.get(i);
				if (move.origin instanceof ImmOperand) {
					int val = ((ImmOperand) move.origin).value;
					REGISTER target = move.target.reg;
					
					boolean clear = true;
					for (int a = i + 1; a < body.instructions.size(); a++) {
						if (body.instructions.get(a) instanceof ASMBranch ||
							body.instructions.get(a) instanceof ASMLabel) {
							clear = false;
							break;
						}
						
						/* Substitute immediate value into move */
						if (body.instructions.get(a) instanceof ASMMov) {
							ASMMov move0 = (ASMMov) body.instructions.get(a);
							if (move0.target.reg == target) break;
							
							if (move0.origin instanceof RegOperand && ((RegOperand) move0.origin).reg == target) {
								move0.origin = new ImmOperand(val);
								OPT_DONE = true;
							}
						}
						else if (body.instructions.get(a) instanceof ASMMvn) {
							ASMMvn move0 = (ASMMvn) body.instructions.get(a);
							if (move0.target.reg == target) break;
							
							if (move0.origin instanceof RegOperand && ((RegOperand) move0.origin).reg == target) {
								move0.origin = new ImmOperand(val);
								OPT_DONE = true;
							}
						}
						else if (body.instructions.get(a) instanceof ASMDataP) {
							ASMDataP dataP = (ASMDataP) body.instructions.get(a);
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
							for (RegOperand r : p.operands) {
								if (r.reg == target) {
									break;
								}
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
	
	/**
	 * Removes non-referenced Labels
	 */
	public void clearUnusedLabels(AsNBody body) {
		List<ASMLabel> usedLabels = new ArrayList();
		
		for (int i = 1; i < body.instructions.size(); i++) {
			ASMInstruction ins = body.instructions.get(i);
			if (ins instanceof ASMLabel) {
				if (((ASMLabel) ins).name.equals("main")) usedLabels.add((ASMLabel) ins);
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
	
	/**
	 * b .L1
	 * ... <- Remove these Lines until Label
	 */
	public void clearInstructionsAfterBranch(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) body.instructions.get(i);
				if (b.cond == null && b.type != BRANCH_TYPE.BL) {
					while (i < body.instructions.size() - 1 && !(body.instructions.get(i + 1) instanceof ASMLabel)) {
						body.instructions.remove(i + 1);
						OPT_DONE = true;
					}
				}
			}
		}
	}
	
}
