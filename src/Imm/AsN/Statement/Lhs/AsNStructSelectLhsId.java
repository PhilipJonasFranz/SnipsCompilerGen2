package Imm.AsN.Statement.Lhs;

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
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNStructSelect;

public class AsNStructSelectLhsId extends AsNLhsId {

	public static AsNStructSelectLhsId cast(StructSelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNStructSelectLhsId id = new AsNStructSelectLhsId();
		lhs.castedNode = id;
		
		AsNStructSelect.injectAddressLoader(id, lhs.select, r, map, st);
		
		if (lhs.select.getType().wordsize() > 1) {
			/* Copy memory section */
			copyFromStack(id, lhs.select.getType().wordsize(), st);
			
			/* Create dummy stack entries for newly copied struct on stack */
			for (int i = 0; i < lhs.select.getType().wordsize(); i++) st.push(REGISTER.R0);
		}
		else {
			/* Load */
			ASMStr store = new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1));
			store.comment = new ASMComment("Store value to struct field");
			id.instructions.add(store);
		}
		
		return id;
	}
	
	public static void copyFromStack(AsNNode node, int size, StackSet st) throws CGEN_EXCEPTION {
		
		/* Do it sequentially for 8 or less words to copy */
		if (size <= 8) {
			int offset = 0;
			
			for (int a = 0; a < size; a++) {
				node.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
				node.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
				offset += 4;
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			/* Move counter in R2 */
			node.instructions.add(new ASMMov(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1)));
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(size * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			node.instructions.add(new ASMCmp(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
			/* Branch to loop end */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
			
			/* Pop value from stack and store it */
			node.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			node.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2), new ImmOperand(4)));
			
			/* Branch to loop start */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(loopStart)));
			
			node.instructions.add(loopEnd);
		}
		
		st.popXWords(size);
	}
	
}
