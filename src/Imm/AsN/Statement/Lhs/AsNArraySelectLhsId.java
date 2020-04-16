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
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNArraySelect;
import Imm.AsN.Expression.AsNArraySelect.SELECT_TYPE;
import Imm.AsN.Statement.AsNAssignment;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNArraySelectLhsId extends AsNLhsId {

	public static AsNArraySelectLhsId cast(ArraySelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNArraySelectLhsId id = new AsNArraySelectLhsId();
		lhs.castedNode = id;
		
		ArraySelect select = lhs.selection;
		
		/* Assign sub Array */
		if (select.getType() instanceof ARRAY) {
			/* Save to param Stack */
			if (st.getParameterByteOffset(select.idRef.origin) != -1) 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.PARAM_SUB, id, select, r, map, st);
			/* Save to global memory */
			else if (map.resolve(select.idRef.origin) != null) 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SUB, id, select, r, map, st);
			/* Save to local stack */
			else 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.LOCAL_SUB, id, select, r, map, st);
		
			/* Data is on stack, copy to location */
			AsNAssignment.copyStackSection(((ARRAY) select.getType()).wordsize(), id);
		}
		/* Assign single array cell */
		else {
			/* Push value on the stack */
			id.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			
			/* Save to param Stack */
			if (st.getParameterByteOffset(select.idRef.origin) != -1) 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, id, select, r, map, st);
			/* Save to global memory */
			else if (map.resolve(select.idRef.origin) != null) 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SINGLE, id, select, r, map, st);
			/* Save to local stack */
			else 
				AsNArraySelect.injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, id, select, r, map, st);
			
			/* Create assign injector */
			if (lhs.assign.assignArith != ASSIGN_ARITH.NONE) {
				/* Pop the value off the stack */
				id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R2)));
				
				id.instructions.add(new ASMLdr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
				
				/* Create assign injector */
				List<ASMInstruction> inj = id.buildInjector(lhs.assign, 1, 2, true, true);
				id.instructions.addAll(inj);
			}
			else 
				/* Pop the value off the stack */
				id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			
			/* Store at target location */
			id.instructions.add(new ASMStr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
		}
	
		return id;
	}
	
}
