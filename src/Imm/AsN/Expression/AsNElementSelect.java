package Imm.AsN.Expression;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
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
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNElementSelect extends AsNExpression {

			/* --- METHODS --- */
	public static AsNElementSelect cast(ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNElementSelect select = new AsNElementSelect();
		s.castedNode = select;
		
		r.free(0, 1, 2);
		
		/* Array is parameter, load from parameter stack */
		if (st.getParameterByteOffset(s.idRef.origin) != -1) {
			if (s.type instanceof ARRAY) {
				injectAddressLoader(SELECT_TYPE.PARAM_SUB, select, s, r, map, st);
				
				/* Loop through array word size and copy values */
				select.instructions.addAll(subArrayCopy(s));
			}
			else {
				injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, select, s, r, map, st);
				
				/* Load */
				select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			}
		}
		else {
			if (s.type instanceof ARRAY) {
				injectAddressLoader(SELECT_TYPE.LOCAL_SUB, select, s, r, map, st);
				
				/* Copy memory location with the size of the array */
				select.instructions.addAll(subArrayCopy(s));
			}
			else {
				injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, select, s, r, map, st);
				
				/* Load */
				select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			}
		}
		
		return select;
	}
	
	public static List<ASMInstruction> loadSumR2(ElementSelect s, RegSet r, MemoryMap map, StackSet st, boolean block) throws CGEN_EXCEPTION {
		List<ASMInstruction> load = new ArrayList();
		
		/* Sum */
		if (s.selection.size() > 1 || block) {
			ASMMov sum = new ASMMov(new RegOperand(REGISTER.R2), new ImmOperand(0));
			sum.comment = new ASMComment("Calculate offset of sub structure");
			load.add(sum);
			
			ARRAY superType = (ARRAY) s.idRef.origin.type;
			
			/* Handle selections differently, since last selection does not result in primitive. 
			 * 		This algorithm is a custom variation of the loadSumR2 method. */
			for (int i = 0; i < s.selection.size(); i++) {
				load.addAll(AsNExpression.cast(s.selection.get(i), r, map, st).getInstructions());
				
				if (superType.elementType instanceof PRIMITIVE) {
					load.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
				}
				else {
					int bytes = superType.elementType.wordsize() * 4;
					
					load.add(new ASMMov(new RegOperand(REGISTER.R1), new ImmOperand(bytes)));
					load.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
				}
				
				/* Add to sum */
				load.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R0)));
				
				/* Next element in chain */
				if (!(superType.elementType instanceof ARRAY)) break;
				else superType = (ARRAY) superType.elementType;
			}
		}
		else {
			/* Load Index and multiply with 4 to convert index to byte offset */
			load.addAll(AsNExpression.cast(s.selection.get(0), r, map, st).getInstructions());
			load.add(new ASMLsl(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		}
		
		return load;
	}
	
	/**
	 * Copy memory location the size of the word size of the type of s, assumes that the start
	 * of the sub structure is located in R1.
	 */
	public static List<ASMInstruction> subArrayCopy(ElementSelect s) {
		List<ASMInstruction> copy = new ArrayList();
		
		ARRAY arr = (ARRAY) s.type;
		int offset = arr.wordsize() * 4;
		/* Do it sequentially for 8 or less words to copy */
		if (arr.wordsize() <= 8) {
			boolean r0 = false;
			for (int a = 0; a < arr.wordsize(); a++) {
				if (!r0) {
					copy.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
				    r0 = true;
				}
				else {
					copy.add(new ASMLdr(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
					copy.add(new ASMPushStack(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2)));
					r0 = false;
				}
				offset -= 4;
			}
			
			if (r0) {
				copy.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			/* Move counter in R2 */
			copy.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(arr.wordsize() * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			copy.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			copy.add(new ASMCmp(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
			/* Branch to loop end */
			copy.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
			
			/* Load value and push it on the stack */
			copy.add(new ASMLdrStack(MEM_OP.POST_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(4)));
			copy.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			
			/* Branch to loop start */
			copy.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(loopStart)));
			
			copy.add(loopEnd);
		}
		
		return copy;
	}
	
	public enum SELECT_TYPE {
		LOCAL_SINGLE, LOCAL_SUB,
		PARAM_SINGLE, PARAM_SUB;
	}
	
	/**
	 * Method is shared with AsNAssignment.<br>
	 * This method calculates the start of an array or sub array either on the local or parameter stack, and
	 * moves the result in R0 if the target is a single cell, or into R1 if the target is a sub structure.
	 */
	public static void injectAddressLoader(SELECT_TYPE selectType, AsNNode node, ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		
		int offset = 0;
		if (selectType == SELECT_TYPE.LOCAL_SINGLE || selectType == SELECT_TYPE.LOCAL_SUB) {
			offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
			offset += (s.idRef.origin.type.wordsize() - 1) * 4;
		}
		else offset = st.getParameterByteOffset(s.idRef.origin);
		
		if (selectType == SELECT_TYPE.LOCAL_SINGLE) {
			/* Load offset of target in array */
			node.instructions.addAll(loadSumR2(s, r, map, st, false));
			
			/* Load offset of array in memory */
			node.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			
			/* Location - block offset */
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2)));
		}
		else if (selectType == SELECT_TYPE.LOCAL_SUB) {
			/* Load block offset */
			node.instructions.addAll(loadSumR2(s, r, map, st, true));
			
			/* Load the start of the structure into R1 */
			ASMSub sub = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset));
			sub.comment = new ASMComment("Start of structure in stack");
			node.instructions.add(sub);
		
			/* Sub the offset to the start of the sub structure from the start in R1 */
			ASMAdd block = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2));
			block.comment = new ASMComment("Start of sub structure in stack");
			node.instructions.add(block);
		}
		else if (selectType == SELECT_TYPE.PARAM_SINGLE) {
			/* Load offset of target in array */
			node.instructions.addAll(loadSumR2(s, r, map, st, false));
				
			/* Get offset of parameter relative to fp */
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
			
			/* Subtract offset */
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2)));
		}
		else if (selectType == SELECT_TYPE.PARAM_SUB) {
			node.instructions.addAll(loadSumR2(s, r, map, st, true));
			
			ASMAdd start = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset));
			start.comment = new ASMComment("Start of structure in stack");
			node.instructions.add(start);
			
			/* Subtract sum */
			node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		}
	}
	
}
