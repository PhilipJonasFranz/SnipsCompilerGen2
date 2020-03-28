package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMLdrStack;
import Imm.ASM.Stack.ASMMemOp.MEM_OP;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;

public class AsNIdRef extends AsNExpression {

	public static AsNIdRef cast(IDRef i, RegSet r, StackSet st, int target) throws CGEN_EXCEPTION {
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
					// TODO No free reg to move copy to, save in stack
					throw new CGEN_EXCEPTION(i.getSource(), "Cannot save copy of value, RegStack overflow!");
				}
			}
			else if (location != target) {
				/* Copy value in target reg */
				ref.instructions.add(new ASMMov(new RegOperand(target), new RegOperand(location)));
				r.copy(location, target);
			}
		}
		else {
			/* Free target reg */
			if (!r.getReg(target).isFree()) {
				int free = r.findFree();
				if (free != -1) {
					/* Free Register, copy value of target reg to free location */
					ref.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(target)));
					r.copy(target, free);
					r.free(target);
				}
				else {
					/* RegStack is full, make Space in Reg Stack, copy value to StackSet */
					// TODO
				}
			}
			
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
	
}
