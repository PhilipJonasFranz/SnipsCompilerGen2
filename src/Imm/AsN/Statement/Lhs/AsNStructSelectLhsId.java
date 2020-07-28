package Imm.AsN.Statement.Lhs;

import java.util.List;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNStructSelect;

public class AsNStructSelectLhsId extends AsNLhsId {

	public static AsNStructSelectLhsId cast(StructSelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNStructSelectLhsId id = new AsNStructSelectLhsId();
		lhs.castedNode = id;
		
		if (lhs.select.getType().wordsize() == 1) {
			id.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
		}
		
		/* Inject the address loader, loads address into R1 */
		AsNStructSelect.injectAddressLoader(id, lhs.select, r, map, st);
		
		if (lhs.select.getType().wordsize() > 1) {
			/* Copy memory section */
			copyFromStack(id, lhs.select.getType().wordsize(), st);
			
			/* Create dummy stack entries for newly copied struct on stack */
			for (int i = 0; i < lhs.select.getType().wordsize(); i++) st.push(REG.R0);
		}
		else {
			/* Load value from stack */
			id.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
			
			if (lhs.assign.assignArith != ASSIGN_ARITH.NONE) {
				/* Load current value in R2 */
				id.instructions.add(new ASMLdr(new RegOp(REG.R2), new RegOp(REG.R1)));
				
				/* Create injector, new result is in R0 */
				List<ASMInstruction> inj = id.buildInjector(lhs.assign, 2, 0, false, true);
				id.instructions.addAll(inj);
			}
			
			/* Store value to location */
			ASMStr store = new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1));
			store.comment = new ASMComment("Store value to struct field");
			id.instructions.add(store);
		}
		
		return id;
	}
	
	/**
	 * Pop size words of the stack and store it at the location specified in R1.
	 * Pops the words of the stack set.
	 */
	public static void copyFromStack(AsNNode node, int size, StackSet st) throws CGEN_EXC {
		/* Do it sequentially for 8 or less words to copy */
		if (size <= 8) {
			int offset = 0;
			
			for (int a = 0; a < size; a++) {
				node.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
				node.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.R1), new ImmOp(offset)));
				offset += 4;
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			/* Move counter in R2 */
			node.instructions.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.R1)));
			node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(size * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			node.instructions.add(new ASMCmp(new RegOp(REG.R1), new RegOp(REG.R2)));
			
			/* Branch to loop end */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(loopEnd)));
			
			/* Pop value from stack and store it */
			node.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
			node.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.R2), new ImmOp(4)));
			
			/* Branch to loop start */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(loopStart)));
			
			node.instructions.add(loopEnd);
		}
		
		st.popXWords(size);
	}
	
} 
