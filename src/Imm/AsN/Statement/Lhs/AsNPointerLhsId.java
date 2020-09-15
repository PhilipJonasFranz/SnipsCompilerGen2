package Imm.AsN.Statement.Lhs;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Deref;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.AsNAssignment;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNPointerLhsId extends AsNLhsId {

	public static AsNPointerLhsId cast(PointerLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNPointerLhsId id = new AsNPointerLhsId();
		lhs.castedNode = id;

		Deref dref = lhs.deref;
		
		/* Store single cell, push value in R0 */
		if (lhs.expressionType instanceof PRIMITIVE || lhs.expressionType instanceof POINTER) {
			id.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
		}
		
		r.free(0);
		
		/* Load target address */
		id.instructions.addAll(AsNExpression.cast(dref.expression, r, map, st).getInstructions());
		
		/* Convert to bytes */
		id.instructions.add(new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R0), new ImmOp(2)));
		
		if (lhs.assign.value.getType() instanceof PRIMITIVE || lhs.assign.value.getType() instanceof POINTER) {
			id.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
			
			/* Create assign injector */
			if (lhs.assign.assignArith != ASSIGN_ARITH.NONE) {
				id.instructions.add(new ASMLdr(new RegOp(REG.R2), new RegOp(REG.R1)));
				
				/* Create assign injector, save address in R1 */
				List<ASMInstruction> inj = id.buildInjector(lhs.assign, 2, 0, false, true);
				id.instructions.addAll(inj);
			}
			
			id.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
		else AsNAssignment.copyStackSection(lhs.assign.value.getType().wordsize(), id, st);
		
		return id;
	}
	
} 
