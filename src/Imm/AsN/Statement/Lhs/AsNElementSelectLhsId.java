package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Lhs.ElementSelectLhsId;
import Imm.AsN.Expression.AsNElementSelect;
import Imm.AsN.Expression.AsNElementSelect.SELECT_TYPE;
import Imm.AsN.Statement.AsNAssignment;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNElementSelectLhsId extends AsNLhsId {

	public static AsNElementSelectLhsId cast(ElementSelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNElementSelectLhsId id = new AsNElementSelectLhsId();
		lhs.castedNode = id;
		
		ElementSelect select = lhs.selection;
		
		/* Assign sub Array */
		if (select.type instanceof ARRAY) {
			/* Save to param Stack */
			if (st.getParameterByteOffset(select.idRef.origin) != -1) 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.PARAM_SUB, id, select, r, map, st);
			/* Save to global memory */
			else if (map.resolve(select.idRef.origin) != null) 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SUB, id, select, r, map, st);
			/* Save to local stack */
			else 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.LOCAL_SUB, id, select, r, map, st);
		
			/* Data is on stack, copy to location */
			AsNAssignment.copyArray((ARRAY) select.type, id);
		}
		/* Assign single array cell */
		else {
			/* Push value on the stack */
			id.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
			
			/* Save to param Stack */
			if (st.getParameterByteOffset(select.idRef.origin) != -1) 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, id, select, r, map, st);
			/* Save to global memory */
			else if (map.resolve(select.idRef.origin) != null) 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SINGLE, id, select, r, map, st);
			/* Save to local stack */
			else 
				AsNElementSelect.injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, id, select, r, map, st);
			
			/* Pop the value off the stack and store it at the target location */
			id.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			id.instructions.add(new ASMStr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
		}
	
		return id;
	}
	
}
