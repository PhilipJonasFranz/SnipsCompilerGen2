package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Res.Const;

public class AsNIDRef extends AsNExpression {

			/* --- METHODS --- */
	public static AsNIDRef cast(IDRef i, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNIDRef ref = new AsNIDRef();
		i.castedNode = ref;
		
		/* Declaration is already loaded in Reg Stack */
		if (r.declarationLoaded(i.origin)) {
			int location = r.declarationRegLocation(i.origin);
			
			/* Declaration is loaded in target reg, make copy */
			if (location == target) {
				int free = r.findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in target reg */
					ref.instructions.add(new ASMMov(new RegOp(free), new RegOp(target)));
					r.copy(target, free);
				}
				else {
					/* No free reg to move copy to, save in stack */
					ref.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOp(target), new RegOp(REG.SP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -4)));
					st.push(i.origin);
				}
			}
			else if (location != target) {
				/* Copy value in target reg */
				ref.instructions.add(new ASMMov(new RegOp(target), new RegOp(location)));
				r.copy(location, target);
			}
		}
		/* Load declaration from global memory */
		else if (map.declarationLoaded(i.origin)) {
			ref.clearReg(r, st, target);
			
			if (i.origin.getType() instanceof PRIMITIVE || i.origin.getType() instanceof POINTER || i.origin.getType() instanceof INTERFACE) {
				/* Load value from memory */
				
				ASMDataLabel label = map.resolve(i.origin);
				
				/* Load memory address */
				ASMLdrLabel ins = new ASMLdrLabel(new RegOp(target), new LabelOp(label), i.origin);
				ins.comment = new ASMComment("Load from .data section");
				ref.instructions.add(ins);
				
				ref.instructions.add(new ASMLdr(new RegOp(target), new RegOp(target)));
			}
			else {
				/* Copy on stack */
				StackUtil.loadToStackFromDeclaration(ref, i, r, map, st);
			}
		}
		/* Load from Stack */
		else {
			/* Load copy on stack */
			if (!(i.origin.getType() instanceof PRIMITIVE || i.origin.getType() instanceof POINTER || i.origin.getType() instanceof INTERFACE)) {
				StackUtil.loadToStackFromDeclaration(ref, i, r, map, st);
			}
			/* Load in register */
			else {
				ref.clearReg(r, st, target);
				
				if (st.getParameterByteOffset(i.origin) != -1) {
					/* Variable is parameter in stack, get offset relative to Frame Pointer in Stack, 
					 * 		Load from Stack */
					int off = st.getParameterByteOffset(i.origin);
					ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, off)));
				}
				else if (st.getDeclarationInStackByteOffset(i.origin) != -1) {
					/* Load Declaration Location from Stack */
					int off = st.getDeclarationInStackByteOffset(i.origin);
					ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -off)));
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
				
				r.getReg(target).setDeclaration(i.origin);
			}
		}
		
		return ref;
	}
	
} 
