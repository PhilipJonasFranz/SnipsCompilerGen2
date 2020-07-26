package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Deref;

public class AsNDeref extends AsNExpression {

			/* --- METHODS --- */
	public static AsNDeref cast(Deref a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNDeref ref = new AsNDeref();
		a.castedNode = ref;
		
		ref.clearReg(r, st, 0, 1);
		
		/* Load Expression */
		ref.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
		
		/* Load from memory, load into R0 */
		if (a.getType().wordsize() == 1) {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOp(target), new RegOp(REG.R0), new ImmOp(2));
			lsl.comment = new ASMComment("Convert to bytes");
			ref.instructions.add(lsl);
			
			ASMLdr load = new ASMLdr(new RegOp(target), new RegOp(target));
			load.comment = new ASMComment("Load from address");
			ref.instructions.add(load);
		}
		/* Load on stack */
		else {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R0), new ImmOp(2));
			lsl.comment = new ASMComment("Convert to bytes");
			ref.instructions.add(lsl);
			
			/* Sequentially push words on stack */
			for (int i = 0; i < a.getType().wordsize(); i++) {
				ASMLdr load = new ASMLdr(new RegOp(target), new RegOp(REG.R1), new ImmOp((a.getType().wordsize() - i - 1) * 4));
				ref.instructions.add(load);
				ref.instructions.add(new ASMPushStack(new RegOp(target)));
				
				/* Push dummy values on stack */
				st.push(REG.R0);
			}
		}
		
		return ref;
	}
	
}
