package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.AsNAssignment;

public class AsNPointerLhsId extends AsNLhsId {

	public static AsNPointerLhsId cast(PointerLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNPointerLhsId id = new AsNPointerLhsId();
		lhs.castedNode = id;

		Deref dref = lhs.deref;
		
		/* Store single cell */
		if (lhs.expressionType.wordsize() == 1) {
			id.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.free(0);
		}
		
		/* Load target address */
		id.instructions.addAll(AsNExpression.cast(dref.expression, r, map, st).getInstructions());
		id.instructions.add(new ASMLsl(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		
		if (lhs.expressionType.wordsize() == 1) {
			id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			id.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		else {
			AsNAssignment.copyArray(lhs.expressionType.wordsize(), id);
		}
		
		return id;
	}
	
}
