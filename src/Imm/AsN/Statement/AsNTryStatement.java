package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNTryStatement extends AsNCompoundStatement {

	ASMLabel watchpointLabel;
	
	public static AsNTryStatement cast(TryStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNTryStatement tr0 = new AsNTryStatement();
		s.castedNode = tr0;
		
		/* Create the watchpoint jump target label */
		tr0.watchpointLabel = new ASMLabel(LabelGen.getLabel());
		
		/* Save stack pointer for watchpoint */
		tr0.instructions.add(new ASMPushStack(new RegOperand(REGISTER.SP)));
		st.push(REGISTER.SP);
		
		// TODO: Make statements in the body jump to the watchpoint
		/* Insert the body */
		tr0.addBody(s, r, map, st);
		
		/* Branch to end, no exception occured */
		ASMLabel endBranch = new ASMLabel(LabelGen.getLabel());
		tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endBranch)));
		
		/* --- INSERT WATCHPOINT --- */
		tr0.instructions.add(tr0.watchpointLabel);
		
		/* Backup SP, currently points to top address of thrown exception */
		tr0.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.SP)));
		
		/* Load SP before the try section was entered */
		int off = st.getHighestSPBackupOffset();
		ASMLdrStack reset = new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.SP), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.DOWN, -off));
		reset.comment = new ASMComment("Load backed up sp from stack");
		tr0.instructions.add(reset);
		
		/* Pop SP Backup */
		tr0.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(4)));
		st.popXWords(1);
		
		for (WatchStatement w : s.watchpoints) {
			/* Skip to this position if thrown exception is not the one watched here */
			ASMLabel skip = new ASMLabel(LabelGen.getLabel());
			
			/* Check if value in R12 matches watched SID */
			STRUCT watched = (STRUCT) w.watched.getType();
			tr0.instructions.add(new ASMCmp(new RegOperand(REGISTER.R12), new ImmOperand(watched.typedef.SID)));
			tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(skip)));
			
			tr0.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(watched.wordsize() * 4)));
			tr0.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(watched.wordsize() * 4)));
			
			tr0.copyReturnStack(tr0, st);
			
			st.push(w.watched);
			
			/* Add body of watcher */
			tr0.addBody(w, r, map, st);
			
			/* Add wordsize of exception to stack */
			tr0.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(watched.wordsize() * 4)));
			
			/* Clear exception flag */
			tr0.instructions.add(new ASMMov(new RegOperand(REGISTER.R12), new ImmOperand(0)));
			
			tr0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endBranch)));
			
			tr0.instructions.add(skip);
		}
		
		tr0.instructions.add(endBranch);
		
		return tr0;
	}
	
	public void copyReturnStack(AsNNode node, StackSet st) throws CGEN_EXCEPTION {
		ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
		loopStart.comment = new ASMComment("Copy stack return with loop");
		
		node.instructions.add(loopStart);
		
		ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
		
		/* Check if whole sub array was loaded */
		node.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
		
		/* Branch to loop end */
		node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
		
		node.instructions.add(new ASMLdrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(-4)));
		
		node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R2)));
		
		
		/* Decrement counter */
		node.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(4)));
		
		/* Branch to loop start */
		node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(loopStart)));
		
		node.instructions.add(loopEnd);
	}
	
}
