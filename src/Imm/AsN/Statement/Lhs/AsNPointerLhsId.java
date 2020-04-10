package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AsN.Expression.AsNElementSelect;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;

public class AsNPointerLhsId extends AsNLhsId {

	public static AsNPointerLhsId cast(PointerLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNPointerLhsId id = new AsNPointerLhsId();
		lhs.castedNode = id;

		Deref dref = lhs.deref;
		
		if (dref.expression instanceof IDRef) {
			
		}
		else if (dref.expression instanceof ElementSelect) {
			ElementSelect select = (ElementSelect) dref.expression;
			
			POINTER p = (POINTER) select.type;
					
			if (p.targetType.wordsize() == 1) {
				/* Push value on stack */
				id.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				
				AsNElementSelect.loadSumR2(id, select, r, map, st, false);
				
				/* Load Pointer Address */
				AsNElementSelect.loadPointer(id, select, r, map, st, 1);
				
				/* Target address */
				id.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
				
				id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
				
				id.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			}
			else {
				AsNElementSelect.loadSumR2(id, select, r, map, st, true);
				
				/* Load Pointer Address */
				AsNElementSelect.loadPointer(id, select, r, map, st, 1);
				
				/* Target address */
				id.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
				
				/* Store */
				ARRAY arr = (ARRAY) p.targetType;
				
				int offset = 0;
				for (int a = 0; a < arr.wordsize(); a++) {
					ASMLdr load = new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.SP), new ImmOperand(offset));
					if (a == 0) load.comment = new ASMComment("Copy the array to the target location");
					id.instructions.add(load);
					id.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(4)));
					offset += 4;
				}
			}
			
		}
		else {
			/* Arithmetic */
		}
		
		return id;
	}
	
}
