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
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;

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
			ref.freeTargetReg(target, r, st);
			
			ASMDataLabel label = map.resolve(i.origin);
			
			/* Load memory address */
			ref.instructions.add(new ASMLdrLabel(new RegOperand(target), new LabelOperand(label)));
			
			/* Load value from memory */
			ref.instructions.add(new ASMLdr(new RegOperand(target), new RegOperand(target)));
		}
		else {
			ref.freeTargetReg(target, r, st);
			
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
		
		return ref;
	}
	
	protected void freeTargetReg(int target, RegSet r, StackSet st) {
		/* Free target reg */
		if (!r.getReg(target).isFree()) {
			int free = r.findFree();
			if (free != -1) {
				/* Free Register, copy value of target reg to free location */
				this.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(target)));
				r.copy(target, free);
			}
			else {
				/* RegStack is full, push copy to StackSet */
				this.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(target), new RegOperand(REGISTER.SP), new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
				st.push(r.getReg(target).declaration);
			}
		}
	}
	
}
