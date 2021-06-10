package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.*;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Imm.AST.Expression.IDRef;
import Imm.AST.Lhs.SimpleLhsId;
import Res.Const;

public class AsNSimpleLhsId extends AsNLhsId {

	public static AsNSimpleLhsId cast(SimpleLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNSimpleLhsId id = new AsNSimpleLhsId().pushCreatorStack(lhs);

		IDRef ref = lhs.ref;
		
		/* Declaration loaded in RegSet, just move value into register */
		if (r.declarationLoaded(ref.origin)) {
			int reg = r.declarationRegLocation(ref.origin);
			id.instructions.add(new ASMMov(new RegOp(reg), new RegOp(0)));
		}
		/* Declaration loaded in VFP, just move value into register */
		else if (r.getVRegSet().declarationLoaded(ref.origin)) {
			int reg = r.getVRegSet().declarationRegLocation(ref.origin);
			id.instructions.add(new ASMVMov(new VRegOp(reg), new VRegOp(0)));
		}
		/* Variable is global variable and type is primitive, store to memory.
		 * 		Other types in memory are handled down below. */
		else if (map.declarationLoaded(ref.origin)) {
			ASMDataLabel label = map.resolve(ref.origin);
			
			/* Load memory address */
			id.instructions.add(new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), ref.origin));
			
			if (ref.origin.getType().wordsize() == 1) {
				/* Store computed to memory */
				id.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1)));
			}
			else 
				/* Copy the value on the stack to the desired location */
				StackUtil.copyToAddressFromStack(ref.origin.getType().wordsize(), id, st);
		}
		/* Store to stack */
		else {
			if (ref.origin.getType().isRegType()) {
				int off = st.getDeclarationInStackByteOffset(ref.origin);
				
				id.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), 
					new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			}
			else if (ref.origin.getType().isStackType()) {
				/* 
				 * Use slight variations of the addressing injector from AsNElementSelect, since we 
				 * dont have to add the sum to the sub structure.
				 */
				
				/* Parameter */
				if (st.getParameterByteOffset(ref.origin) != -1) {
					int offset = st.getParameterByteOffset(ref.origin);
					
					ASMAdd start = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset));
					id.instructions.add(start.com("Start of structure in stack"));
				}
				else if (map.resolve(ref.origin) != null) {
					ASMDataLabel label = map.resolve(ref.origin);
					
					ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), ref.origin);
					id.instructions.add(load.com("Load data section address"));
				}
				/* Local */
				else if (st.getDeclarationInStackByteOffset(ref.origin) != -1) {
					int offset = st.getDeclarationInStackByteOffset(ref.origin);
					offset += (ref.origin.getType().wordsize() - 1) * 4;
					
					/* Load the start of the structure into R1 */
					ASMSub sub = new ASMSub(new RegOp(REG.R1), new RegOp(REG.FP), new ImmOp(offset));
					id.instructions.add(sub.com("Start of structure in stack"));
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
				
				/* Copy array */
				StackUtil.copyToAddressFromStack((lhs.origin.getType()).wordsize(), id, st);
			}
			else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
		}
		
		r.free(0, 1, 2);

		return id.popCreatorStack();
	}
	
} 
