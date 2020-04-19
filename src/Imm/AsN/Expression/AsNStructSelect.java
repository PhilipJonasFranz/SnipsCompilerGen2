package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.StructSelect;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNStructSelect extends AsNExpression {
	
			/* --- METHODS --- */
	public static AsNStructSelect cast(StructSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNStructSelect sel = new AsNStructSelect();
		s.castedNode = sel;
		
		/* Create a address loader that points to the first word of the target */
		injectAddressLoader(sel, s, r, map, st);
		
		if (s.getType().wordsize() > 1) {
			/* Copy memory section */
			AsNArraySelect.subStructureCopy(sel, s.getType().wordsize());
			
			/* Create dummy stack entries for newly copied struct on stack */
			for (int i = 0; i < s.getType().wordsize(); i++) st.push(REGISTER.R0);
		}
		else {
			/* Load */
			ASMLdr load = new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1));
			load.comment = new ASMComment("Load field from struct");
			sel.instructions.add(load);
		}
		
		return sel;
	}
	
	public static void injectAddressLoader(AsNNode node, StructSelect select, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Load base address */
		if (select.selector instanceof IDRef) {
			IDRef ref = (IDRef) select.selector;
			
			if (r.declarationLoaded(ref.origin)) {
				int loc = r.declarationRegLocation(ref.origin);
				
				/* Just move in R1 */
				node.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(loc)));
			}
			else if (st.getDeclarationInStackByteOffset(ref.origin) != -1) {
				/* In Local Stack */
				int offset = st.getDeclarationInStackByteOffset(ref.origin);
				offset += (ref.origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				node.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			}
			else if (st.getParameterByteOffset(ref.origin) != -1) {
				/* In Parameter Stack */
				int offset = st.getParameterByteOffset(ref.origin);
				
				ASMAdd start = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset));
				start.comment = new ASMComment("Start of structure in stack");
				node.instructions.add(start);
			}
			else if (map.declarationLoaded(ref.origin)) {
				/* In Global Memory */
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load data label */
				node.instructions.add(new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label)));
			}
		}
		
		if (select.deref) {
			ASMLsl lsl = new ASMLsl(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(2));
			lsl.comment = new ASMComment("Convert to bytes");
			node.instructions.add(lsl);
		}
		
		if (!node.instructions.isEmpty())
			node.instructions.get(0).comment = new ASMComment("Load field location");
		
		/* Base address is now in R1 */
		
		/* Base Type */
		StructSelect sel0 = select;
		
		while (true) {
			/* Current selector type */
			TYPE type = sel0.selector.getType();
			
			/* Current selection */
			Expression selection = sel0.selection;
			
			/* Unwrap pointer */
			if (type instanceof POINTER && sel0.deref) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			
			if (type instanceof STRUCT) {
				/* Cast to struct type */
				STRUCT struct = (STRUCT) type;
				
				if (selection instanceof StructSelect) {
					StructSelect sel1 = (StructSelect) selection;
					
					if (sel1.selector instanceof IDRef) {
						injectIDRef(node, struct, (IDRef) sel1.selector);
					}
					else if (sel1.selector instanceof ArraySelect) {
						injectArraySelect(node, (ArraySelect) sel1.selector, r, map, st);
					}
				}
				/* Base Case */
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					injectIDRef(node, struct, (IDRef) ref);
				}
				else if (selection instanceof ArraySelect) {
					injectArraySelect(node, (ArraySelect) selection, r, map, st);
				}
			}

			/* If current selection derefs and its not the last selection in the chain */
			if (sel0.selection instanceof StructSelect) {
				StructSelect sel1 = (StructSelect) sel0.selection;
				if (sel1.deref) {
					/* Deref, just load current address */
					node.instructions.add(new ASMLdr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1)));
					node.instructions.add(new ASMLsl(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(2)));
				}
			}
			
			/* Keep selecting */
			if (selection instanceof StructSelect) {
				sel0 = (StructSelect) sel0.selection;
			}
			/* Base Case reached */
			else break;
		}
	}
	
	private static void injectArraySelect(AsNNode node, ArraySelect arr, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Push current on stack */
		node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R1)));
		
		/* Load the struture offset in R2 */
		if (arr.getType().wordsize() > 1)
			AsNArraySelect.loadSumR2(node, arr, r, map, st, true);
		else AsNArraySelect.loadSumR2(node, arr, r, map, st, false);
		
		node.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
		
		/* Add sum to current */
		node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
	}
	
	private static void injectIDRef(AsNNode node, STRUCT struct, IDRef ref) {
		int offset = struct.getFieldByteOffset(ref.id);
		if (offset != 0) node.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
	}
	
}