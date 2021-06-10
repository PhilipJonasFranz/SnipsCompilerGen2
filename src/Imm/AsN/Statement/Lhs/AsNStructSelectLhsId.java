package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.REG;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AsN.Expression.AsNStructSelect;

public class AsNStructSelectLhsId extends AsNLhsId {

	public static AsNStructSelectLhsId cast(StructSelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNStructSelectLhsId id = new AsNStructSelectLhsId().pushCreatorStack(lhs);

		if (lhs.select.getType().wordsize() == 1) 
			id.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
		
		/* Inject the address loader, loads address into R1 */
		AsNStructSelect.injectAddressLoader(id, lhs.select, r, map, st, false);
		
		if (lhs.select.getType().wordsize() > 1) {
			/* Copy memory section */
			StackUtil.copyToAddressFromStack(lhs.select.getType().wordsize(), id, st);
			
			/* Create dummy stack entries for newly copied struct on stack */
			for (int i = 0; i < lhs.select.getType().wordsize(); i++) st.push(REG.R0);
		}
		else {
			/* Load value from stack */
			id.instructions.add(new ASMPopStack(new RegOp(REG.R0)));
			
			/* Store value to location */
			ASMStr store = new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1));
			id.instructions.add(store.com("Store value to struct field"));
		}

		return id.popCreatorStack();
	}
	
} 
