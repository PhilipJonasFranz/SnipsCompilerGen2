package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
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
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNStructSelect extends AsNExpression {
	
			/* --- METHODS --- */
	public static AsNStructSelect cast(StructSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNStructSelect sel = new AsNStructSelect();
		s.castedNode = sel;
		
		/* Load base address */
		if (s.selector instanceof IDRef) {
			IDRef ref = (IDRef) s.selector;
			
			if (r.declarationLoaded(ref.origin)) {
				int loc = r.declarationRegLocation(ref.origin);
				
				/* Just move in R0 */
				sel.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(loc)));
			}
			else if (st.getDeclarationInStackByteOffset(ref.origin) != -1) {
				/* In Local Stack */
				int offset = st.getDeclarationInStackByteOffset(ref.origin);
				offset += (ref.origin.type.wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				sel.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			}
			else if (st.getParameterByteOffset(ref.origin) != -1) {
				/* In Parameter Stack */
				int offset = st.getParameterByteOffset(ref.origin);
				
				ASMAdd start = new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset));
				start.comment = new ASMComment("Start of structure in stack");
				sel.instructions.add(start);
			}
			else if (map.declarationLoaded(ref.origin)) {
				/* In Global Memory */
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load data label */
				sel.instructions.add(new ASMLdrLabel(new RegOperand(REGISTER.R0), new LabelOperand(label)));
			}
		}
		
		if (s.deref) {
			ASMLsl lsl = new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2));
			lsl.comment = new ASMComment("Convert to bytes");
			sel.instructions.add(lsl);
		}
		
		if (!sel.instructions.isEmpty())
			sel.instructions.get(0).comment = new ASMComment("Load field location");
		
		/* Base address is now in R0 */
		
		/* Base Type */
		StructSelect sel0 = s;
		
		while (true) {
			/* Current selector type */
			TYPE type = sel0.selector.type;
			
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
						sel.injectIDRef(struct, (IDRef) sel1.selector);
					}
					else if (sel1.selector instanceof ArraySelect) {
						// TODO
					}
				}
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					sel.injectIDRef(struct, (IDRef) ref);
				}
			}

			/* If current selection derefs and its not the last selection in the chain */
			if (sel0.deref && !(sel0.selector instanceof IDRef)) {
				/* Deref, just load current address */
				sel.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			}
			
			if (selection instanceof IDRef) {
				break;
			}
			else {
				sel0 = (StructSelect) sel0.selection;
			}
			
		}
		
		if (s.type.wordsize() > 1) {
			/* Move address in R1 */
			sel.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			
			/* Copy memory section */
			// TODO
		}
		else {
			/* Load */
			ASMLdr load = new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0));
			load.comment = new ASMComment("Load field");
			sel.instructions.add(load);
		}
		
		return sel;
	}
	
	public void injectIDRef(STRUCT struct, IDRef ref) {
		int offset = struct.getFieldByteOffset(ref.id);
		if (offset != 0) this.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(offset)));
	}
	
}
