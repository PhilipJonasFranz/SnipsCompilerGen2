package Imm.AsN.Expression;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
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

			/* --- METHODS --- */
	public static AsNTempAtom cast(TempAtom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNTempAtom atom = new AsNTempAtom();
		a.castedNode = atom;
		
		/* Free as many regs as needed */
		if (a.base == null || (a.base != null && a.base.getType().wordsize() == 1 && a.inheritType.wordsize() == 1))
			r.free(0);
		else if (a.base != null && a.base.getType().wordsize() == 1 && a.inheritType.wordsize() == 2)
			r.free(0, 1);
		else 
			r.free(0, 1, 2);
		
		if (a.base == null) {
			/* Make space on stack */
			if (a.getType().wordsize() > 1) {
				/* Override last value and insert SID */
				if (a.getType() instanceof STRUCT && !CompilerDriver.disableStructSIDHeaders) {
					STRUCT s = (STRUCT) a.getType();
					
					atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp((a.getType().wordsize() - 1) * 4)));
					
					atom.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(s.getTypedef().SID)));
					atom.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
					
					for (int i = 0; i < a.getType().wordsize(); i++)
						st.push(REG.R0);
				}
				else {
					atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(a.getType().wordsize() * 4)));
					
					for (int i = 0; i < a.getType().wordsize(); i++)
						st.push(REG.R0);
				}
			}
			else {
				/* Dont need to do anything here */
			}
		}
		else {
			if (a.base.getType().wordsize() > 1) {
				int range = a.inheritType.wordsize() / a.base.getType().wordsize();
				
				atom.instructions.addAll(AsNExpression.cast(a.base, r, map, st).getInstructions());
				
				ASMLabel start = null;
				
				ASMLabel end = null;
				
				if (range - 1 > 1) {
					ASMMov mov = new ASMMov(new RegOp(REG.R1), new ImmOp(0));
					mov.comment = new ASMComment("Copy substructure with loop " + (range - 1) + " times");
					atom.instructions.add(mov);
					
					start = new ASMLabel(LabelGen.getLabel());
					
					end = new ASMLabel(LabelGen.getLabel());
					
					atom.instructions.add(start);
					
					atom.instructions.add(new ASMCmp(new RegOp(REG.R1), new ImmOp(range - 1)));
					
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(end)));
				}
				
				for (int k = 0; k < a.base.getType().wordsize(); k++) {
					atom.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.SP), new ImmOp(k * 4)));
					atom.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.SP), new ImmOp((k - a.base.getType().wordsize()) * 4)));
				}
				
				atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(a.base.getType().wordsize() * 4)));
				
				if (range - 1 > 1) {
					atom.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(1)));
					
					atom.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(start)));
					
					atom.instructions.add(end);
				}
				
				for (int i = 0; i < range - 1; i++)
					for (int k = 0; k < a.base.getType().wordsize(); k++) 
						st.push(REG.R0);
			}
			else {
				/* Cast the base expression */
				atom.instructions.addAll(AsNExpression.cast(a.base, r, map, st).getInstructions());
				
				if (a.getType().wordsize() > 1) {
					/* Propagate data into paralell registers to make use of ldm */
					if (a.getType().wordsize() > 1) 
						atom.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.R0)));
					if (a.getType().wordsize() > 2) 
						atom.instructions.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.R0)));
					
					int regs = 0;
					for (int i = 0; i < a.inheritType.wordsize(); i++) {
						/* If the inherited type is a struct and SIDs are enabled and this is the last word to be pushed, override value to SID */
						if (a.inheritType instanceof STRUCT && !CompilerDriver.disableStructSIDHeaders && i == a.inheritType.wordsize() - 1) {
							STRUCT s = (STRUCT) a.inheritType;
							atom.instructions.add(new ASMMov(new RegOp(regs), new ImmOp(s.getTypedef().SID)));
						}
						
						if (regs == 3) {
							AsNStructureInit.flush(regs, atom);
							regs = 0;
						}
						
						regs++;
						st.push(REG.R0);
					}
					
					AsNStructureInit.flush(regs, atom);
				}
				else {
					if (target != 0)
						atom.instructions.add(new ASMMov(new RegOp(target), new RegOp(REG.R0)));
				}
			}
		}
		
		return atom;
	}
	
} 
