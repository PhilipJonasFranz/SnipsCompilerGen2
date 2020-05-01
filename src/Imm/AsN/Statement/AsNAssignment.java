package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
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
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Assignment;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.Lhs.AsNLhsId;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNAssignment assign = new AsNAssignment();
		
		/* Compute value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
		if (!assign.instructions.isEmpty()) assign.instructions.get(0).comment = new ASMComment("Evaluate Expression");
		
		/* Store value at location specified by lhs */
		assign.instructions.addAll(AsNLhsId.cast(a.lhsId, r, map, st).getInstructions());
		
		return assign;
	}
	
	/**
	 * Assumes that the base address of the array or the start address of the memory section is located in R1.
	 * Pops the word it copies of the stack.
	 * @param size The amound of words to copy.
	 * @throws CGEN_EXCEPTION 
	 */
	public static void copyStackSection(int size, AsNNode node, StackSet st) throws CGEN_EXCEPTION {
		/* Do it sequentially for 8 or less words to copy */
		if (size <= 8) {
			int offset = 0;
			for (int a = 0; a < size; a++) {
				/* Pop data from stack */
				node.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
				
				node.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
				
				offset += 4;
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			// TODO: Test this implementation
			
			/* Move counter in R2 */
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(size * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			node.instructions.add(new ASMCmp(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
			/* Branch to loop end */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
			
			/* Pop value from stack and store it at location */
			node.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			
			node.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(4)));
			
			/* Branch to loop start */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(loopStart)));
			
			node.instructions.add(loopEnd);
		}
		
		st.popXWords(size);
	}
	
}
