package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.ArraySelect;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Res.Const;

public class AsNArraySelect extends AsNExpression {

			/* ---< NESTED >--- */
	public enum SELECT_TYPE {
		REG_SINGLE, REG_SUB,
		LOCAL_SINGLE, LOCAL_SUB,
		PARAM_SINGLE, PARAM_SUB,
		GLOBAL_SINGLE, GLOBAL_SUB;
	}
	
	
			/* ---< METHODS >--- */
	public static AsNArraySelect cast(ArraySelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNArraySelect select = new AsNArraySelect();
		select.pushOnCreatorStack();
		s.castedNode = select;
		
		r.free(0, 1, 2);
		
		if (r.declarationLoaded(s.idRef.origin)) {
			if (s.getType().wordsize() > 1) 
				injectAddressLoader(SELECT_TYPE.REG_SUB, select, s, r, map, st);
			else 
				injectAddressLoader(SELECT_TYPE.REG_SINGLE, select, s, r, map, st);
		}
		else if (st.getParameterByteOffset(s.idRef.origin) != -1) {
			/* Array is parameter, load from parameter stack */
			if (s.getType().wordsize() > 1) 
				injectAddressLoader(SELECT_TYPE.PARAM_SUB, select, s, r, map, st);
			else 
				injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, select, s, r, map, st);
		}
		else if (map.declarationLoaded(s.idRef.origin)) {
			/* Data Memory */
			if (s.getType().wordsize() > 1) 
				injectAddressLoader(SELECT_TYPE.GLOBAL_SUB, select, s, r, map, st);
			else 
				injectAddressLoader(SELECT_TYPE.GLOBAL_SINGLE, select, s, r, map, st);
		}
		else {
			if (s.getType().wordsize() > 1) 
				injectAddressLoader(SELECT_TYPE.LOCAL_SUB, select, s, r, map, st);
			else 
				injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, select, s, r, map, st);
		}
		
		if (s.getType().wordsize() > 1) {
			/* Loop through array word size and copy values */
			StackUtil.copyToStackFromAddress(select, s.getType().wordsize());
			
			/* Push dummy values on the stack */
			st.pushDummies(s.getType().wordsize());
		}
		else {
			/* Load single value into R0 */
			select.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
		}
		
		select.registerMetric();
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
			if (s.idRef.getType().isPointer()) {
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
				if (!superType.elementType.isArray()) break;
				else superType = (ARRAY) superType.elementType;
			}
		}
		else {
			/* Load Index and multiply with 4 to convert index to byte offset */
			node.instructions.addAll(AsNExpression.cast(s.selection.get(0), r, map, st).getInstructions());
			node.instructions.add(new ASMLsl(new RegOp(REG.R2), new RegOp(REG.R0), new ImmOp(2)));
			
			/* 
			 * Searches the type the array consists of. The wordsize of the type is then multiplied with
			 * the index. If the type, which is selected from, is a pointer, we get the target of the pointer.
			 * If the resulting type is an array, we also get the element type of the array, since we can
			 * assume we deref and select an index in this expression.
			 */
			TYPE targetType = s.idRef.getType().getContainedType();
			if (s.idRef.getType().isPointer() && targetType.isArray()) 
				targetType = targetType.getContainedType();
		
			/* Multiply with type wordsize */
			if (targetType.wordsize() > 1) {
				/* Move wordsize in R0 */
				node.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(targetType.wordsize())));
				/* Multiply with word Size */
				node.instructions.add(new ASMMult(new RegOp(REG.R2), new RegOp(REG.R2), new RegOp(REG.R0)));
			}
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
		
		if (selectType == SELECT_TYPE.REG_SINGLE) {
			/* Load offset of target in array */
			loadSumR2(node, s, r, map, st, false);
			
			int loc = r.declarationRegLocation(s.idRef.origin);
			node.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(loc), new ImmOp(2)));
			
			/* Location - block offset */
			node.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R2)));
		}
		else if (selectType == SELECT_TYPE.REG_SUB) {
			/* Load block offset */
			loadSumR2(node, s, r, map, st, true);
			
			int loc = r.declarationRegLocation(s.idRef.origin);
			
			node.instructions.add(new ASMLsl(new RegOp(REG.R1), new RegOp(loc), new ImmOp(2)));
			
			/* Sub the offset to the start of the sub structure from the start in R1 */
			ASMAdd block = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2));
			block.comment = new ASMComment("Start of sub structure in stack");
			node.instructions.add(block);
		}
		else if (selectType == SELECT_TYPE.LOCAL_SINGLE) {
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
		else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
	}
	
} 
