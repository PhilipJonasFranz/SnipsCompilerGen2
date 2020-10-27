package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.TempAtom;
import Imm.TYPE.COMPOSIT.STRUCT;
import Snips.CompilerDriver;

public class AsNTempAtom extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTempAtom cast(TempAtom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNTempAtom atom = new AsNTempAtom();
		a.castedNode = atom;

		int SID = (a.getType() instanceof STRUCT)? ((STRUCT) a.inheritType).getTypedef().SID : -1;
		boolean pushSID = SID != -1 && !CompilerDriver.disableStructSIDHeaders;
		
		/* Free as many regs as needed */
		if (a.base == null || (a.base != null && a.base.getType().wordsize() == 1 && a.inheritType.wordsize() == 1))
			r.free(0);
		else if (a.base != null && a.base.getType().wordsize() == 1 && a.inheritType.wordsize() == 2)
			r.free(0, 1);
		else 
			r.free(0, 1, 2);
		
		/* Temp atom is an absolute placeholder */
		if (a.base == null) {
			/* Make space on stack */
			if (a.getType().wordsize() > 1) {
				/* Override last value and insert SID */
				if (pushSID) {
					/* Make space on stack */
					atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp((a.getType().wordsize() - 1) * 4)));
					
					/* Insert SID */
					atom.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(SID)));
					atom.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				}
				else 
					/* Only make space on stack, no SID */
					atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(a.getType().wordsize() * 4)));
				
				/* Push dummies to align stack */
				st.pushDummies(a.getType().wordsize());
			}
			else {
				/* Value would be placed in register, no need to do anything here */
			}
		}
		/* Temp atom fills with values defined by the base expression */
		else {
			if (a.base.getType().wordsize() > 1) {

				/*
				 * TODO:
				 * 
				 * Decrement SP, load structure, but offsetted by one so it starts at the SID.
				 * The copy the structure as often as required, but stop when hitting the base.
				 */
				
				/* Amount of times the structure defined by the base expression has to be copied */
				int range = a.inheritType.wordsize() / a.base.getType().wordsize();
				
				/* Cast the base expression once */
				atom.instructions.addAll(AsNExpression.cast(a.base, r, map, st).getInstructions());
				
				ASMLabel start = null;
				
				ASMLabel end = null;
				
				/* 
				 * If the structure needs to be copied multiple times, inject a check that compares
				 * against a counter that counts the times the structure has been copied already.
				 */
				if (range > 2) {
					ASMMov mov = new ASMMov(new RegOp(REG.R1), new ImmOp(0));
					mov.comment = new ASMComment("Copy substructure with loop " + (range - 1) + " times");
					atom.instructions.add(mov);
					
					start = new ASMLabel(LabelUtil.getLabel());
					
					end = new ASMLabel(LabelUtil.getLabel());
					
					atom.instructions.add(start);
					
					atom.instructions.add(new ASMCmp(new RegOp(REG.R1), new ImmOp(range - 1)));
					
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(end)));
				}
				
				/* Copy the stack section sequentially */
				for (int k = 0; k < a.base.getType().wordsize(); k++) {
					atom.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.SP), new ImmOp(k * 4)));
					atom.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.SP), new ImmOp((k - a.base.getType().wordsize()) * 4)));
				}
				
				/* Decrement the stack pointer by the size of the copied structure */
				atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(a.base.getType().wordsize() * 4)));
				
				/* Increment the counter and jump to the loop start */
				if (range - 1 > 1) {
					atom.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(1)));
					
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(start)));
					
					atom.instructions.add(end);
				}
				
				/* 
				 * Push dummies on the stack, but only range - 1 times the size, 
				 * since once the dummies were pushed during casting. 
				 */
				st.pushDummies((range - 1) * a.base.getType().wordsize());
				
				/* If required, replace top word on the stack with the SID */
				if (pushSID) {
					
					// TODO: This leaves out the top word of the structure cut-off */
					
					/* Insert SID */
					atom.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(SID)));
					atom.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.SP)));
				}
			}
			/* Base is one word large */
			else {
				/* Cast the base expression once */
				atom.instructions.addAll(AsNExpression.cast(a.base, r, map, st).getInstructions());
				
				/* Single word needs to be duplicated multiple times */
				if (a.getType().wordsize() > 1) {
					/* Propagate data into paralell registers to make use of ldm */
					if (a.getType().wordsize() > 1) 
						atom.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.R0)));
					if (a.getType().wordsize() > 2) 
						atom.instructions.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.R0)));
					
					int regs = 0;
					for (int i = 0; i < a.inheritType.wordsize(); i++) {
						/* If the inherited type is a struct and SIDs are enabled and this is the last word to be pushed, override value to SID */
						if (pushSID && i == a.inheritType.wordsize() - 1) 
							atom.instructions.add(new ASMMov(new RegOp(regs), new ImmOp(SID)));
						
						if (regs == 3) {
							AsNStructureInit.flush(regs, atom);
							regs = 0;
						}
						
						regs++;
						st.pushDummy();
					}
					
					AsNStructureInit.flush(regs, atom);
				}
				else 
					/* If the target type is only one large, we should not be required to push the SID */
					assert !pushSID : "Pushing the SID at this point is impossible!";
					
					/* If the target was not R0, we need to move it there from R0. Otherwise we are done. */
					if (target != 0) atom.instructions.add(new ASMMov(new RegOp(target), new RegOp(REG.R0)));
			}
		}
		
		return atom;
	}
	
} 
