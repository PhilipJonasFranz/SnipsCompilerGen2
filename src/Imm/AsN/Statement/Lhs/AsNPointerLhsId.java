package Imm.AsN.Statement.Lhs;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.AsNAssignment;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNPointerLhsId extends AsNLhsId {

	public static AsNPointerLhsId cast(PointerLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNPointerLhsId id = new AsNPointerLhsId();
		lhs.castedNode = id;

		Deref dref = lhs.deref;
		
		/* Store single cell */
		if (lhs.expressionType instanceof PRIMITIVE) {
			id.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			r.free(0);
		}
		
		/* Load target address */
		id.instructions.addAll(AsNExpression.cast(dref.expression, r, map, st).getInstructions());
		id.instructions.add(new ASMLsl(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		
		if (lhs.expressionType instanceof PRIMITIVE) {
			id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			
			/* Create assign injector */
			if (lhs.assign.assignArith != ASSIGN_ARITH.NONE) {
				id.instructions.add(new ASMLdr(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1)));
				
				/* Create assign injector, save address in R1 */
				List<ASMInstruction> inj = id.buildInjector(lhs.assign, 2, 0, false, true);
				id.instructions.addAll(inj);
			}
			
			id.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		else AsNAssignment.copyStackSection(lhs.expressionType.wordsize(), id, st);
		
		return id;
	}
	
}
