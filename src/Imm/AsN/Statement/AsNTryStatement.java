package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNTryStatement extends AsNCompoundStatement {

	ASMLabel watchpointLabel;
	
	public static AsNTryStatement cast(TryStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNTryStatement tr0 = new AsNTryStatement();
		s.castedNode = tr0;
		
		/* Create the watchpoint jump target label */
		tr0.watchpointLabel = new ASMLabel(LabelGen.getLabel());
		
		/* Save stack pointer for watchpoint */
		tr0.instructions.add(new ASMPushStack(new RegOp(REG.SP)));
		st.push(REG.SP);
	
		/* Insert the body */
		tr0.addBody(s, r, map, st);
		
		/* Load SP before the try section was entered, no exception was thrown, 
		 * dont pop from stack yet since offset is needed below */
		tr0.loadSPBackup(tr0, st);
		
		/* Branch to end, no exception occured */
		ASMLabel endBranch = new ASMLabel(LabelGen.getLabel());
		tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endBranch)));
		
		/* --- INSERT WATCHPOINT --- */
		tr0.instructions.add(tr0.watchpointLabel);
		
		/* Backup SP, currently points to top address of thrown exception */
		tr0.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.SP)));
		
		/* Load SP before the try section was entered, exception was thrown */
		tr0.loadSPBackup(tr0, st);
		st.popXWords(1);
		
		for (WatchStatement w : s.watchpoints) {
			/* Skip to this position if thrown exception is not the one watched here */
			ASMLabel skip = new ASMLabel(LabelGen.getLabel());
			
			/* Check if value in R12 matches watched SID */
			STRUCT watched = (STRUCT) w.watched.getType();
			tr0.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(watched.getTypedef().SID)));
			tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOp(skip)));
			
			tr0.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(watched.wordsize() * 4)));
			tr0.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(watched.wordsize() * 4)));
			
			AsNBody.branchToCopyRoutine(tr0);
			
			/* Add the declaration of the exception to the stack, is already loaded */
			st.push(w.watched);
			
			/* Add body of watcher */
			tr0.addBody(w, r, map, st);
			
			/* Add wordsize of exception to stack */
			tr0.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(watched.wordsize() * 4)));
			st.pop();
			
			/* Clear exception flag */
			tr0.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
			
			tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endBranch)));
			
			tr0.instructions.add(skip);
		}
		
		if (!s.unwatched.isEmpty()) {
			tr0.instructions.add(new ASMComment("Unwatched Exceptions"));
			
			/* For each unwatched type, compare the SID and move the corresponding size in R0 */
			for (TYPE t : s.unwatched) {
				STRUCT s0 = (STRUCT) t;
				tr0.instructions.add(new ASMCmp(new RegOp(REG.R12), new ImmOp(s0.getTypedef().SID)));
				tr0.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(s0.wordsize() * 4), new Cond(COND.EQ)));
			}
			
			/* R1 contains the location of the start of the exception in the stack, move in SP to inject in copy loop */
			tr0.instructions.add(new ASMMov(new RegOp(REG.SP), new RegOp(REG.R1)));
			
			/* Exception was not caught, escape to higher watchpoint */
			AsNSignalStatement.injectWatchpointBranch(tr0, s.watchpoint, null);
		}
		
		tr0.instructions.add(endBranch);
		
		tr0.freeDecs(r, s);
		return tr0;
	}
	
	public void loadSPBackup(AsNNode node, StackSet st) {
		int off = st.getHighestSPBackupOffset();
		ASMLdrStack reset = new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.SP), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off));
		reset.comment = new ASMComment("Load backed up sp from stack");
		node.instructions.add(reset);
		
		/* Pop SP Backup */
		node.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(4)));
	}
	
} 
