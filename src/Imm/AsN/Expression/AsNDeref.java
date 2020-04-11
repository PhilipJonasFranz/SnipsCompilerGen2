package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;

public class AsNDeref extends AsNExpression {

			/* --- METHODS --- */
	public static AsNDeref cast(Deref a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNDeref ref = new AsNDeref();
		a.castedNode = ref;
		
		ref.clearReg(r, st, 0);
		
		/* Load Expression */
		ref.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
		
		/* Convert to bytes */
		ref.instructions.add(new ASMLsl(new RegOperand(target), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		
		/* Load from memory */
		ref.instructions.add(new ASMLdr(new RegOperand(target), new RegOperand(target)));
		
		return ref;
	}
	
}