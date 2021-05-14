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
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.TempAtom;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.COMPOSIT.STRUCT;
import Snips.CompilerDriver;

public class AsNTempAtom extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTempAtom cast(TempAtom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNTempAtom atom = new AsNTempAtom();
		atom.pushOnCreatorStack(a);
		a.castedNode = atom;

		STRUCT struct = null;
		StructTypedef def = null;
		
		if (a.getType() instanceof STRUCT) {
			struct = (STRUCT) a.getType();
			def = ((STRUCT) a.inheritType).getTypedef();
		}
		
		boolean pushSID = def != null && !CompilerDriver.disableStructSIDHeaders;
		
		/* Free as many regs as needed */
		if (a.base == null || a.base.getType().wordsize() == 1 && a.inheritType.wordsize() == 1)
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
					def.loadSIDInReg(atom, REG.R0, struct.proviso);
					
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

				int wordsToStore = a.getType().wordsize() - a.base.getType().wordsize();
				if (pushSID) wordsToStore--;
				
				/* 
				 * Aligns the stack so that when casting the base, the top of 
				 * the base is aligned to the head of the final memory section 
				 */
				atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(wordsToStore * 4)));
				
				st.pushDummies(wordsToStore);
			
				/* Cast the base expression once */
				atom.instructions.addAll(AsNExpression.cast(a.base, r, map, st).getInstructions());
				
				if (wordsToStore > 0) {
					/* Points to start of base in stack */
					atom.instructions.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.SP)));
					
					/* Counter that keeps track of how many words still need to be copied */
					atom.instructions.add(new ASMMov(new RegOp(REG.R1), new ImmOp(wordsToStore)));
					
					ASMLabel start = new ASMLabel(LabelUtil.getLabel());
					
					ASMLabel end = new ASMLabel(LabelUtil.getLabel());
					
					/* Start of loop */
					atom.instructions.add(start);
					
					/* Counter is zero, branch to end */
					atom.instructions.add(new ASMCmp(new RegOp(REG.R1), new ImmOp(0)));
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, COND.EQ, new LabelOp(end)));
					
					/* Load current dataword */
					atom.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R2)));
					
					/* Store the word below the end of the complete structure */
					atom.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R2), new ImmOp(a.base.getType().wordsize() * 4)));
					
					/* Slide down to next data word */
					atom.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R2), new ImmOp(4)));
					
					/* Decrement counter */
					atom.instructions.add(new ASMSub(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(1)));
					
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(start)));
					
					atom.instructions.add(end);
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
							def.loadSIDInReg(atom, new RegOp(regs).reg, struct.proviso);

						if (regs == 3) {
							AsNStructureInit.flush(regs, atom, a.base.getType().isFloat());
							regs = 0;
						}
						
						regs++;
						st.pushDummy();
					}
					
					AsNStructureInit.flush(regs, atom, a.base.getType().isFloat());
				}
				else 
					/* If the target type is only one large, we should not be required to push the SID */
					assert !pushSID : "Pushing the SID at this point is impossible!";
					
					/* If the target was not R0, we need to move it there from R0. Otherwise we are done. */
					if (target != 0) atom.instructions.add(new ASMMov(new RegOp(target), new RegOp(REG.R0)));
			}
		}
		
		atom.registerMetric();
		return atom;
	}
	
} 
