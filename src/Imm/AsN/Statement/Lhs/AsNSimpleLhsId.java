package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
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
import Imm.AST.Expression.IDRef;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AsN.Statement.AsNAssignment;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNSimpleLhsId extends AsNLhsId {

	public static AsNSimpleLhsId cast(SimpleLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNSimpleLhsId id = new AsNSimpleLhsId();
		lhs.castedNode = id;

		IDRef ref = lhs.ref;
		
		/* Declaration already loaded, just move value into register */
		if (r.declarationLoaded(ref.origin)) {
			int reg = r.declarationRegLocation(ref.origin);
			id.instructions.add(new ASMMov(new RegOperand(reg), new RegOperand(0)));
		}
		/* Variable is global variable and type is primitive, store to memory.
		 * 		Other types in memory are handled down below. */
		else if (map.declarationLoaded(ref.origin) && ref.origin.type instanceof PRIMITIVE) {
			ASMDataLabel label = map.resolve(ref.origin);
			
			/* Load memory address */
			id.instructions.add(new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label)));
				
			/* Store computed to memory */
			id.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		/* Store to stack */
		else {
			if (ref.origin.type instanceof PRIMITIVE) {
				int off = st.getDeclarationInStackByteOffset(ref.origin);
				id.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), 
					new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
			}
			else if (ref.origin.type instanceof ARRAY) {
				/* Use light variations of the addressing injector from AsNElementSelect, since we 
				 * dont have to add the sum to the sub structure.
				 */
				
				/* Parameter */
				if (st.getParameterByteOffset(ref.origin) != -1) {
					int offset = st.getParameterByteOffset(ref.origin);
					
					ASMAdd start = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset));
					start.comment = new ASMComment("Start of structure in stack");
					id.instructions.add(start);
				}
				else if (map.resolve(ref.origin) != null) {
					ASMDataLabel label = map.resolve(ref.origin);
					
					ASMLdrLabel load = new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label));
					load.comment = new ASMComment("Load data section address");
					id.instructions.add(load);
				}
				/* Local */
				else {
					int offset = st.getDeclarationInStackByteOffset(ref.origin);
					offset += (ref.origin.type.wordsize() - 1) * 4;
					
					/* Load the start of the structure into R1 */
					ASMSub sub = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset));
					sub.comment = new ASMComment("Start of structure in stack");
					id.instructions.add(sub);
				}
				
				/* Copy array */
				AsNAssignment.copyArray(((ARRAY) lhs.origin.type).wordsize(), id);
			}
		}
		
		r.free(0, 1, 2);
		
		return id;
	}
	
}
