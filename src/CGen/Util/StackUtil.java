package CGen.Util;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.IDRef;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNStructureInit;
import Res.Const;

/**
 * Contains utility functions related to stack operations, like copying a memory
 * section on the stack, or storing to a memory location from the stack.
 */
public class StackUtil {
	
	/**
	 * Assumes that the base address of the array or the start address of the memory section is located in R1.
	 * Pops the word it copies of the stack.
	 * @param size The amound of words to copy.
	 */
	public static void copyToAddressFromStack(int size, AsNNode node, StackSet st) throws CGEN_EXC {
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
			
			ASMLabel loopStart = new ASMLabel(LabelUtil.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelUtil.getLabel());
			
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

	/**
	 * Copy memory location of the given wordsize, assumes that the start
	 * of the sub structure is located in R1. Push the copied section on the stack.
	 */
	public static void copyToStackFromAddress(AsNNode node, int size) {
		
		/* Do it sequentially for 8 or less words to copy */
		if (size <= 8) {
			int offset = (size - 1) * 4;
			
			boolean r0 = false;
			for (int a = 0; a < size; a++) {
				if (!r0) {
					node.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R1), new ImmOp(offset)));
				    r0 = true;
				}
				else {
					node.instructions.add(new ASMLdr(new RegOp(REG.R2), new RegOp(REG.R1), new ImmOp(offset)));
					node.instructions.add(new ASMPushStack(new RegOp(REG.R2), new RegOp(REG.R0)));
					r0 = false;
				}
				offset -= 4;
			}
			
			if (r0) {
				node.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			/* Move counter in R2 */
			node.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R1), new ImmOp(size * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelUtil.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelUtil.getLabel());
			
			/* Check if whole sub array was loaded */
			node.instructions.add(new ASMCmp(new RegOp(REG.R1), new RegOp(REG.R2)));
			
			/* Branch to loop end */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(loopEnd)));
			
			/* Load value and push it on the stack */
			node.instructions.add(new ASMLdrStack(MEM_OP.POST_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.R1), new ImmOp(4)));
			node.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
			
			/* Branch to loop start */
			node.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(loopStart)));
			
			node.instructions.add(loopEnd);
		}
	}
	
	/**
	 * Load the current value from any id-ref on the stack, and push dummy R0 values.
	 * The addressing is determined on the origin in the id-ref and where it is currently loaded.
	 * 
	 * @param node The node the instructions are added to.
	 * @param i The id-ref from whiches origin the value is loaded.
	 * @param r The current register-set.
	 * @param map The current memory map.
	 * @param st The current stack-set.
	 */
	public static void loadToStackFromDeclaration(AsNNode node, IDRef i, RegSet r, MemoryMap map, StackSet st) {
		int wordSize = i.getType().wordsize();
		
		r.free(0);
		
		/* Origin is in parameter stack */
		if (st.getParameterByteOffset(i.origin) != -1) {
			int offset = st.getParameterByteOffset(i.origin);
			offset += (wordSize - 1) * 4;
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 3) {
					node.instructions.add(new ASMLdr(new RegOp(regs), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, node);
					regs = 0;
				}
				offset -= 4;
				st.push(REG.R0);
			}
			
			AsNStructureInit.flush(regs, node);
		}
		/* Origin is in global map */
		else if (map.declarationLoaded(i.origin)) {
			ASMDataLabel label = map.resolve(i.origin);
			
			ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R2), new LabelOp(label), i.origin);
			load.comment = new ASMComment("Load data section address");
			node.instructions.add(load);
			
			node.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R2), new ImmOp((wordSize - 1) * 4)));
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 2) {
					node.instructions.add(new ASMLdrStack(MEM_OP.POST_WRITEBACK, new RegOp(regs), new RegOp(REG.R2), new ImmOp(-4)));
					regs++;
				}
				if (regs == 2) {
					AsNStructureInit.flush(regs, node);
					regs = 0;
				}
				st.push(REG.R0);
			}
		}
		/* Origin is in local stack */
		else if (st.getDeclarationInStackByteOffset(i.origin) != -1) {
			int offset = st.getDeclarationInStackByteOffset(i.origin);
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 3) {
					node.instructions.add(new ASMLdr(new RegOp(regs), new RegOp(REG.FP), new ImmOp(-offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, node);
					regs = 0;
				}
				offset += 4;
				st.push(REG.R0);
			}
			
			AsNStructureInit.flush(regs, node);
		}
		else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
	}
	
	/**
	 * Builds the stack copy routine which is used by functions and other program parts.<br>
	 * <br>
	 * The routine assumes that:<br>
	 * <br>
	 * - That the amount of bytes to copy is located in R0.<br>
	 * - That the --> END OF THE MEMORY SECTION <-- address is located in R1.<br>
	 * - That the return address from the routine is located in R10.<br>
	 * <br>
	 * The routine causes:<br>
	 * <br>
	 * - That the datawords are loaded and pushed on the stack.
	 */
	public static List<ASMInstruction> buildStackCopyRoutine() {
		List<ASMInstruction> routine = new ArrayList();
		routine.add(new ASMComment("System Routine, used to copy memory on the stack"));
		
		routine.add(AsNBody.stackCopyRoutine);
		
		ASMLabel loopEnd = new ASMLabel("_routine_stack_copy_end_");
		
		/* Check if whole sub array was loaded */
		routine.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
		
		/* Branch to loop end */
		routine.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(loopEnd)));
		
		routine.add(new ASMLdrStack(MEM_OP.PRE_WRITEBACK, new RegOp(REG.R2), new RegOp(REG.R1), new ImmOp(-4)));
		
		routine.add(new ASMPushStack(new RegOp(REG.R2)));
		
		/* Decrement counter */
		routine.add(new ASMSub(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(4)));
		
		/* Branch to loop start */
		routine.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(AsNBody.stackCopyRoutine)));
		
		routine.add(loopEnd);
		
		/* Branch back */
		routine.add(new ASMMov(new RegOp(REG.PC), new RegOp(REG.R10)));
		routine.add(new ASMSeperator());
		
		return routine;
	}
	
} 
