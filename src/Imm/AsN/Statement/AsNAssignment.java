package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Stack.ASMMemOp.MEM_OP;
import Imm.ASM.Stack.ASMStrStack;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Assignment;
import Imm.AsN.Expression.AsNExpression;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNAssignment assign = new AsNAssignment();
		
		/* Compute value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
		
		/* Declaration already loaded, just move value into register */
		if (r.declarationLoaded(a.origin)) {
			int reg = r.declarationRegLocation(a.origin);
			assign.instructions.add(new ASMMov(new RegOperand(reg), new RegOperand(0)));
		}
		/* Not loaded, store value to stack or into register */
		else {
			int free = r.findFree();
			
			if (free != -1) {
				if (a.value != null) {
					assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
				}
				assign.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(0)));
				r.copy(0, free);
			}
			else {
				/* Store to stack */
				int off = st.getDeclarationInStackByteOffset(a.origin);
				assign.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
			}
		}
		
		return assign;
	}
	
}
