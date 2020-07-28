package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

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
			
			if (i.origin.getType() instanceof PRIMITIVE) {
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
				ref.loadMemorySection(i, r, map, st);
			}
		}
		/* Load from Stack */
		else {
			/* Load copy on stack */
			if (!(i.origin.getType() instanceof PRIMITIVE || i.origin.getType() instanceof POINTER)) {
				ref.loadMemorySection(i, r, map, st);
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
				else {
					/* Load Declaration Location from Stack */
					int off = st.getDeclarationInStackByteOffset(i.origin);
					ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -off)));
				}
				
				r.getReg(target).setDeclaration(i.origin);
			}
		}
		
		return ref;
	}
	
	protected void loadMemorySection(IDRef i, RegSet r, MemoryMap map, StackSet st) {
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
					this.instructions.add(new ASMLdr(new RegOp(regs), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				offset -= 4;
				st.push(REG.R0);
			}
			
			AsNStructureInit.flush(regs, this);
		}
		/* Origin is in global map */
		else if (map.declarationLoaded(i.origin)) {
			ASMDataLabel label = map.resolve(i.origin);
			
			ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R2), new LabelOp(label), i.origin);
			load.comment = new ASMComment("Load data section address");
			this.instructions.add(load);
			
			this.instructions.add(new ASMAdd(new RegOp(REG.R2), new RegOp(REG.R2), new ImmOp((wordSize - 1) * 4)));
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 2) {
					this.instructions.add(new ASMLdrStack(MEM_OP.POST_WRITEBACK, new RegOp(regs), new RegOp(REG.R2), new ImmOp(-4)));
					regs++;
				}
				if (regs == 2) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				st.push(REG.R0);
			}
		}
		/* Origin is in local stack */
		else {
			int offset = st.getDeclarationInStackByteOffset(i.origin);
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 3) {
					this.instructions.add(new ASMLdr(new RegOp(regs), new RegOp(REG.FP), new ImmOp(-offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				offset += 4;
				st.push(REG.R0);
			}
			
			AsNStructureInit.flush(regs, this);
		}
	}
	
} 
