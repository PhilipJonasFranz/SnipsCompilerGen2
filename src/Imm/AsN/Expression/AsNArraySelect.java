package Imm.AsN.Expression;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
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
import Imm.AST.Expression.ArraySelect;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;

public class AsNArraySelect extends AsNExpression {

			/* --- NESTED --- */
	public enum SELECT_TYPE {
		LOCAL_SINGLE, LOCAL_SUB,
		PARAM_SINGLE, PARAM_SUB,
		GLOBAL_SINGLE, GLOBAL_SUB;
	}
	
	
			/* --- METHODS --- */
	public static AsNArraySelect cast(ArraySelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNArraySelect select = new AsNArraySelect();
		s.castedNode = select;
		
		r.free(0, 1, 2);
		
		/* Array is parameter, load from parameter stack */
		if (st.getParameterByteOffset(s.idRef.origin) != -1) {
			if (s.getType() instanceof ARRAY) {
				injectAddressLoader(SELECT_TYPE.PARAM_SUB, select, s, r, map, st);
			}
			else {
				injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, select, s, r, map, st);
			}
		}
		else if (map.declarationLoaded(s.idRef.origin)) {
			/* Data Memory */
			if (s.getType() instanceof ARRAY) {
				injectAddressLoader(SELECT_TYPE.GLOBAL_SUB, select, s, r, map, st);
			}
			else {
				injectAddressLoader(SELECT_TYPE.GLOBAL_SINGLE, select, s, r, map, st);
			}
		}
		else {
			if (s.getType() instanceof ARRAY) {
				injectAddressLoader(SELECT_TYPE.LOCAL_SUB, select, s, r, map, st);
			}
			else {
				injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, select, s, r, map, st);
			}
		}
		
		if (s.getType() instanceof ARRAY) {
			/* Loop through array word size and copy values */
			subStructureCopy(select, ((ARRAY) s.getType()).wordsize());
		}
		else {
			/* Load */
			select.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
		}
		
		return select;
	}
	
	/**
	 * Load the byte offset to the selected sub structure and place it in R2.
	 */
	public static void loadSumR2(AsNNode node, ArraySelect s, RegSet r, MemoryMap map, StackSet st, boolean block) throws CGEN_EXC {
		/* Sum */
		if (s.selection.size() > 1 || block) {
			ASMMov sum = new ASMMov(new RegOp(REG.R2), new ImmOp(0));
			sum.comment = new ASMComment("Calculate offset of sub structure");
			node.instructions.add(sum);
			
			ARRAY superType = null;
			if (s.idRef.getType() instanceof POINTER) {
				superType = (ARRAY) ((POINTER) s.idRef.getType()).targetType;
			}
			else superType = (ARRAY) s.idRef.origin.getType();
			
			/* Handle selections differently, since last selection does not result in primitive. 
			 * 		This algorithm is a custom variation of the loadSumR2 method. */
			for (int i = 0; i < s.selection.size(); i++) {
				node.instructions.addAll(AsNExpression.cast(s.selection.get(i), r, map, st).getInstructions());
				
				if (superType.elementType.wordsize() == 1) {
					node.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
				}
				else {
					int bytes = superType.elementType.wordsize() * 4;
					
					node.instructions.add(new ASMMov(new RegOp(REG.R1), new ImmOp(bytes)));
					node.instructions.add(new ASMMult(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R1)));
				}
				
				/* Add to sum */
				node.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R2), new RegOp(REG.R0)));
				
				/* Next element in chain */
				if (!(superType.elementType instanceof ARRAY)) break;
				else superType = (ARRAY) superType.elementType;
			}
		}
		else {
			/* Load Index and multiply with 4 to convert index to byte offset */
			node.instructions.addAll(AsNExpression.cast(s.selection.get(0), r, map, st).getInstructions());
			node.instructions.add(new ASMLsl(new RegOp(REG.R2), new RegOp(REG.R0), new ImmOp(2)));
		
			/* Multiply with type wordsize */
			int wordSize = s.getType().getCoreType().wordsize();
			if (wordSize > 1) {
				/* Move wordsize in R0 */
				node.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(wordSize)));
				/* Multiply with word Size */
				node.instructions.add(new ASMMult(new RegOp(REG.R2), new RegOp(REG.R2), new RegOp(REG.R0)));
			}
		}
		
	}
	
	/**
	 * Copy memory location the size of the word size of the type of s, assumes that the start
	 * of the sub structure is located in R1. Push the copied section on the stack.
	 */
	public static void subStructureCopy(AsNNode node, int size) {
		
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
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			node.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
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
	 * Method is shared with AsNAssignment.<br>
	 * This method calculates the start of an array or sub array either on the local or parameter stack, and
	 * moves the result in R0 if the target is a single cell, or into R1 if the target is a sub structure.
	 */
	public static void injectAddressLoader(SELECT_TYPE selectType, AsNNode node, ArraySelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		
		int offset = 0;
		if (selectType == SELECT_TYPE.LOCAL_SINGLE || selectType == SELECT_TYPE.LOCAL_SUB) {
			offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
			offset += (s.idRef.origin.getType().wordsize() - 1) * 4;
		}
		else offset = st.getParameterByteOffset(s.idRef.origin);
		
		if (selectType == SELECT_TYPE.LOCAL_SINGLE) {
			/* Load offset of target in array */
			loadSumR2(node, s, r, map, st, false);
			
			/* Load offset of array in memory */
			node.instructions.add(new ASMSub(new RegOp(REG.R0), new RegOp(REG.FP), new ImmOp(offset)));
			
			/* Location - block offset */
			node.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R2)));
		}
		else if (selectType == SELECT_TYPE.LOCAL_SUB) {
			/* Load block offset */
			loadSumR2(node, s, r, map, st, true);
			
			/* Load the start of the structure into R1 */
			ASMSub sub = new ASMSub(new RegOp(REG.R1), new RegOp(REG.FP), new ImmOp(offset));
			sub.comment = new ASMComment("Start of structure in stack");
			node.instructions.add(sub);
		
			/* Sub the offset to the start of the sub structure from the start in R1 */
			ASMAdd block = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2));
			block.comment = new ASMComment("Start of sub structure in stack");
			node.instructions.add(block);
		}
		else if (selectType == SELECT_TYPE.PARAM_SINGLE) {
			/* Load offset of target in array */
			loadSumR2(node, s, r, map, st, false);
			
			/* Get offset of parameter relative to fp */
			node.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
			
			/* Add offset */
			node.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R2)));
		}
		else if (selectType == SELECT_TYPE.PARAM_SUB) {
			loadSumR2(node, s, r, map, st, true);
			
			ASMAdd start = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset));
			start.comment = new ASMComment("Start of structure in stack");
			node.instructions.add(start);
			
			/* Add sum */
			node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2)));
		}
		else if (selectType == SELECT_TYPE.GLOBAL_SINGLE) {
			loadSumR2(node, s, r, map, st, false);
			
			ASMDataLabel label = map.resolve(s.idRef.origin);
			
			ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R0), new LabelOp(label), s.idRef.origin);
			load.comment = new ASMComment("Load data section address");
			node.instructions.add(load);
			
			/* Add sum */
			node.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R2)));
		}
		else if (selectType == SELECT_TYPE.GLOBAL_SUB) {
			loadSumR2(node, s, r, map, st, true);
			
			ASMDataLabel label = map.resolve(s.idRef.origin);
		
			ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), s.idRef.origin);
			load.comment = new ASMComment("Load data section address");
			node.instructions.add(load);
			
			/* Add sum */
			node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2)));
		}
	}
	
	public static void loadPointer(AsNNode node, ArraySelect s, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		node.instructions.addAll(AsNIDRef.cast(s.idRef, r, map, st, target).getInstructions());
		node.instructions.add(new ASMLsl(new RegOp(target), new RegOp(target), new ImmOp(2)));
	}
	
}
