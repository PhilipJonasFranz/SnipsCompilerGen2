package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNIdRef extends AsNExpression {

			/* --- METHODS --- */
	public static AsNIdRef cast(IDRef i, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNIdRef ref = new AsNIdRef();
		i.castedNode = ref;
		
		/* Declaration is already loaded in Reg Stack */
		if (r.declarationLoaded(i.origin)) {
			int location = r.declarationRegLocation(i.origin);
			
			/* Declaration is loaded in target reg, make copy */
			if (location == target) {
				int free = r.findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in target reg */
					ref.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(target)));
					r.copy(target, free);
				}
				else {
					/* No free reg to move copy to, save in stack */
					ref.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(target), new RegOperand(REGISTER.SP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
					st.push(i.origin);
				}
			}
			else if (location != target) {
				/* Copy value in target reg */
				ref.instructions.add(new ASMMov(new RegOperand(target), new RegOperand(location)));
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
				ASMLdrLabel ins = new ASMLdrLabel(new RegOperand(target), new LabelOperand(label), i.origin);
				ins.comment = new ASMComment("Load from .data section");
				ref.instructions.add(ins);
				
				ref.instructions.add(new ASMLdr(new RegOperand(target), new RegOperand(target)));
			}
			else {
				/* Copy on stack */
				ref.loadMemorySection(i, r, map, st);
			}
		}
		/* Load from Stack */
		else {
			/* Load copy on stack */
			if (!(i.origin.getType() instanceof PRIMITIVE)) {
				ref.loadMemorySection(i, r, map, st);
			}
			/* Load in register */
			else {
				ref.clearReg(r, st, target);
				
				if (st.getParameterByteOffset(i.origin) != -1) {
					/* Variable is parameter in stack, get offset relative to Frame Pointer in Stack, 
					 * 		Load from Stack */
					int off = st.getParameterByteOffset(i.origin);
					ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(target), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, off)));
				}
				else {
					/* Load Declaration Location from Stack */
					int off = st.getDeclarationInStackByteOffset(i.origin);
					ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(target), new RegOperand(REGISTER.FP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
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
					this.instructions.add(new ASMLdr(new RegOperand(regs), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				offset -= 4;
				st.push(REGISTER.R0);
			}
			
			AsNStructureInit.flush(regs, this);
		}
		/* Origin is in global map */
		else if (map.declarationLoaded(i.origin)) {
			ASMDataLabel label = map.resolve(i.origin);
			
			ASMLdrLabel load = new ASMLdrLabel(new RegOperand(REGISTER.R2), new LabelOperand(label), i.origin);
			load.comment = new ASMComment("Load data section address");
			this.instructions.add(load);
			
			this.instructions.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R2), new ImmOperand((wordSize - 1) * 4)));
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 2) {
					this.instructions.add(new ASMLdrStack(MEM_OP.POST_WRITEBACK, new RegOperand(regs), new RegOperand(REGISTER.R2), new ImmOperand(-4)));
					regs++;
				}
				if (regs == 2) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				st.push(REGISTER.R0);
			}
		}
		/* Origin is in local stack */
		else {
			int offset = st.getDeclarationInStackByteOffset(i.origin);
			
			/* Copy memory location with the size of the array */
			int regs = 0;
			for (int a = 0; a < wordSize; a++) {
				if (regs < 3) {
					this.instructions.add(new ASMLdr(new RegOperand(regs), new RegOperand(REGISTER.FP), new ImmOperand(-offset)));
					regs++;
				}
				if (regs == 3) {
					AsNStructureInit.flush(regs, this);
					regs = 0;
				}
				offset += 4;
				st.push(REGISTER.R0);
			}
			
			AsNStructureInit.flush(regs, this);
		}
	}
	
}
