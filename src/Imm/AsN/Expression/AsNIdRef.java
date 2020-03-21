package Imm.AsN.Expression;

import CGen.RegSet;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;

public class AsNIdRef extends AsNExpression {

	public AsNIdRef() {
		
	}
	
	public static AsNIdRef cast(IDRef i, RegSet r, int target) {
		AsNIdRef ref = new AsNIdRef();
		
		/* Declaration is already loaded in Reg Stack */
		if (r.declarationLoaded(i.origin)) {
			int location = r.declarationRegLocation(i.origin);
			
			/* Declaration is loaded in R0 */
			if (location == 0) {
				int free = r.findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in R0 */
					ref.instructions.add(new ASMMove(new RegOperand(free), new RegOperand(REGISTER.R0)));
					r.copy(0, free);
				}
				
				if (target != 0) {
					ref.instructions.add(new ASMMove(new RegOperand(free), new RegOperand(target)));
					r.copy(target, free);
				}
			}
			else if (location != 0) {
				/* Copy value in R0 */
				ref.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(location)));
				r.copy(location, target);
			}
		}
		else {
			/* Make space in Reg Stack */
			
			/* Load declaration */
		}
		
		return ref;
	}
	
}
