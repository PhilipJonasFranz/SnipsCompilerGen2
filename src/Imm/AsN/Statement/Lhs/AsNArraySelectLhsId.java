package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNArraySelect;
import Imm.AsN.Expression.AsNArraySelect.SELECT_TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNArraySelectLhsId extends AsNLhsId {

	public static AsNArraySelectLhsId cast(ArraySelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
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
			StackUtil.copyToAddressFromStack(((ARRAY) select.getType()).wordsize(), id, st);
		}
		/* Assign single array cell */
		else {
			/* Push value on the stack */
			id.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
			st.push(REG.R0);
			
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
				id.instructions.add(new ASMPopStack(new RegOp(REG.R2)));
				
				id.instructions.add(new ASMLdr(new RegOp(REG.R1), new RegOp(REG.R0)));
				
				/* Create assign injector */
				id.instructions.addAll(id.buildInjector(lhs.assign, 1, 2, true, true));
			}
			else 
				/* Pop the value off the stack */
				id.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
			
			st.popXWords(1);
			
			/* Store at target location */
			id.instructions.add(new ASMStr(new RegOp(REG.R1), new RegOp(REG.R0)));
		}
	
		return id;
	}
	
} 
