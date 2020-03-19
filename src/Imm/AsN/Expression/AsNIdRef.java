package Imm.AsN.Expression;

import CGen.RegSet;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Expression.IDRef;

public class AsNIdRef extends AsNExpression {

	public AsNIdRef() {
		
	}
	
	public static AsNIdRef cast(IDRef i, RegSet r) {
		AsNIdRef ref = new AsNIdRef();
		
		if (r.declarationLoaded(i.origin)) {
			int location = r.declarationRegLocation(i.origin);
			
			/* Declaration is loaded in target register */
			if (location == 0) {
				int free = r.findFree();
				
				if (free != -1) {
					/* Copy declaration to other free location, leave result in R0 */
					ref.instructions.add(new ASMMove(new RegisterOperand(free), new RegisterOperand(REGISTER.R0)));
					r.copy(0, free);
				}
			}
			else if (location != 0) {
				/* Copy value in R0 */
				ref.instructions.add(new ASMMove(new RegisterOperand(REGISTER.R0), new RegisterOperand(location)));
			}
		}
		
		return ref;
	}
	
}
