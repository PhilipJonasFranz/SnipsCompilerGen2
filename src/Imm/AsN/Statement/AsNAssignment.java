package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.Assignment;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.Lhs.AsNLhsId;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAssignment assign = new AsNAssignment();
		
		/* Compute value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
		if (!assign.instructions.isEmpty()) assign.instructions.get(0).comment = new ASMComment("Evaluate Expression");
		
		/* Store value at location specified by lhs */
		assign.instructions.addAll(AsNLhsId.cast(a.lhsId, r, map, st).getInstructions());
		
		assign.freeDecs(r, a);
		return assign;
	}
	
	/**
	 * Assumes that the base address of the array or the start address of the memory section is located in R1.
	 * Pops the word it copies of the stack.
	 * @param size The amound of words to copy.
	 * @throws CGEN_EXC 
	 */
	public static void copyStackSection(int size, AsNNode node, StackSet st) throws CGEN_EXC {
		/* Do it sequentially for 8 or less words to copy */
		if (size <= 8) {
			int offset = 0;
			for (int a = 0; a < size; a++) {
				/* Pop data from stack */
				node.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
				
				node.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1), new ImmOp(offset)));
				
				offset += 4;
			}
		}
		else {
			if (size * 4 < 255) {
				/* Move counter in R2 */
				node.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R1), new ImmOp(size * 4)));
			}
			else {
				/* Load value via literal manager */
				AsNBody.literalManager.loadValue(node, size * 4, 2);
				
				/* Move counter in R2 */
				node.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R2), new RegOp(REG.R1)));
			}
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			node.instructions.add(new ASMCmp(new RegOp(REG.R1), new RegOp(REG.R2)));
			
			/* Branch to loop end */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(loopEnd)));
			
			/* Pop value from stack and store it at location */
			node.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
			
			node.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.R1), new ImmOp(4)));
			
			/* Branch to loop start */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(loopStart)));
			
			node.instructions.add(loopEnd);
		}
		
		st.popXWords(size);
	}
	
}
