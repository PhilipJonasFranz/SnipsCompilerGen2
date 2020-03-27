package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMLdrStack;
import Imm.ASM.Stack.ASMMemOp.MEM_OP;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;

public class AsNIdRef extends AsNExpression {

	public AsNIdRef() {
		
	}
	
	public static AsNIdRef cast(IDRef i, RegSet r, StackSet st, int target) {
		AsNIdRef ref = new AsNIdRef();
		i.castedNode = ref;
		
		/* Declaration is already loaded in Reg Stack */
		if (r.declarationLoaded(i.origin)) {
			int location = r.declarationRegLocation(i.origin);
			
			/* Declaration is loaded in R0 */
			if (location == 0) {
				int free = r.findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in R0 */
					ref.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(REGISTER.R0)));
					r.copy(0, free);
				}
				
				if (target != 0) {
					ref.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(target)));
					r.copy(target, free);
				}
			}
			else if (location != 0) {
				/* Copy value in R0 */
				ref.instructions.add(new ASMMov(new RegOperand(target), new RegOperand(location)));
				r.copy(location, target);
			}
		}
		else {
			/* Free R0 */
			if (!r.getReg(0).isFree()) {
				int free = r.findFree();
				if (free != -1) {
					/* Free Register, copy value of R0 to free location */
					ref.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(REGISTER.R0)));
					r.copy(0, free);
					r.free(0);
				}
				else {
					/* Make Space in Reg Stack, copy value to StackSet */
					// TODO
				}
			}
			
			if (st.getParameterByteOffset(i.origin) != -1) {
				/* Variable is parameter in stack, get offset relative to Frame Pointer in Stack, 
				 * 		Load from Stack */
				int off = st.getParameterByteOffset(i.origin);
				ref.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new ImmOperand(off)));
			}
			else {
				/* Load Declaration Location from Stack */
			}
			
			r.getReg(0).setDeclaration(i.origin);
		}
		
		return ref;
	}
	
}
