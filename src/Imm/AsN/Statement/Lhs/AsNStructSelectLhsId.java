package Imm.AsN.Statement.Lhs;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Expression.AsNStructSelect;

public class AsNStructSelectLhsId extends AsNLhsId {

	public static AsNStructSelectLhsId cast(StructSelectLhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNStructSelectLhsId id = new AsNStructSelectLhsId();
		lhs.castedNode = id;
		
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
			
			if (lhs.assign.assignArith != ASSIGN_ARITH.NONE) {
				/* Load current value in R2 */
				id.instructions.add(new ASMLdr(new RegOp(REG.R2), new RegOp(REG.R1)));
				
				/* Create injector, new result is in R0 */
				List<ASMInstruction> inj = id.buildInjector(lhs.assign, 2, 0, false, true);
				id.instructions.addAll(inj);
			}
			
			/* Store value to location */
			ASMStr store = new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1));
			store.comment = new ASMComment("Store value to struct field");
			id.instructions.add(store);
		}
		
		return id;
	}
	
} 
