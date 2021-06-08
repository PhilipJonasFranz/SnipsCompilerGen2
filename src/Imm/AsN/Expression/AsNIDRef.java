package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Memory.Stack.ASMVLdrStack;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Imm.AST.Expression.IDRef;
import Res.Const;

public class AsNIDRef extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNIDRef cast(IDRef i, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNIDRef ref = new AsNIDRef().pushCreatorStack(i);

		boolean isVFP = i.origin.getType().isFloat();
		
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
			else {
				/* Copy value in target reg */
				ref.instructions.add(new ASMMov(new RegOp(target), new RegOp(location)));
				r.copy(location, target);
			}
		}
		/* Declaration is already loaded in Reg Stack */
		else if (r.getVRegSet().declarationLoaded(i.origin)) {
			int location = r.getVRegSet().declarationRegLocation(i.origin);
			
			/* Declaration is loaded in target reg, make copy */
			if (location == target) {
				int free = r.getVRegSet().findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in target reg */
					ref.instructions.add(new ASMVMov(new VRegOp(free), new VRegOp(target)));
					r.getVRegSet().copy(target, free);
				}
				else {
					/* No free reg to move copy to, save in stack */
					ref.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new VRegOp(target), new RegOp(REG.SP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -4)));
					st.push(i.origin);
				}
			}
			else {
				/* Copy value in target reg */
				ref.instructions.add(new ASMVMov(new VRegOp(target), new VRegOp(location)));
				r.getVRegSet().copy(location, target);
			}
		}
		/* Load declaration from global memory */
		else if (map.declarationLoaded(i.origin)) {
			ref.clearReg(r, st, false, target);
			
			if (i.origin.getType().isRegType()) {
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
				StackUtil.copyToStackFromDeclaration(ref, i, r, map, st);
			}
		}
		/* Load from Stack */
		else {
			/* Load copy on stack */
			if (!(i.origin.getType().isRegType())) {
				StackUtil.copyToStackFromDeclaration(ref, i, r, map, st);
			}
			/* Load in register */
			else {
				ref.clearReg(r, st, false, target);
				
				if (st.getParameterByteOffset(i.origin) != -1) {
					/* Variable is parameter in stack, get offset relative to Frame Pointer in Stack, 
					 * 		Load from Stack */
					int off = st.getParameterByteOffset(i.origin);
					
					if (isVFP) ref.instructions.add(new ASMVLdrStack(MEM_OP.PRE_NO_WRITEBACK, new VRegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, off)));
					else ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, off)));
				}
				else if (st.getDeclarationInStackByteOffset(i.origin) != -1) {
					/* Load Declaration Location from Stack */
					int off = st.getDeclarationInStackByteOffset(i.origin);
					
					if (isVFP) ref.instructions.add(new ASMVLdrStack(MEM_OP.PRE_NO_WRITEBACK, new VRegOp(target), new RegOp(REG.FP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -off)));
					else ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), 
							new PatchableImmOp(PATCH_DIR.DOWN, -off)));
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
				
				r.getReg(target).setDeclaration(i.origin);
			}
		}

		return ref.popCreatorStack();
	}
	
} 
