package CGen.Opt;

import java.util.ArrayList;
import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AsN.AsNBody;

public class ASMOptimizer {

	public ASMOptimizer() {
		
	}
	
	public void optimize(AsNBody body) {
		boolean OPT_DONE = true;
		
		while (OPT_DONE) {
			OPT_DONE = false;
			
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
	}
	
}
