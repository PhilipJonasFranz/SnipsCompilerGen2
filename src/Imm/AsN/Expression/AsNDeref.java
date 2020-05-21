package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;

public class AsNDeref extends AsNExpression {

			/* --- METHODS --- */
	public static AsNDeref cast(Deref a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNDeref ref = new AsNDeref();
		a.castedNode = ref;
		
		ref.clearReg(r, st, 0, 1);
		
		/* Load Expression */
		ref.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
		
		/* Load from memory */
		if (a.getType().wordsize() == 1) {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOperand(target), new RegOperand(REGISTER.R0), new ImmOperand(2));
			lsl.comment = new ASMComment("Convert to bytes");
			ref.instructions.add(lsl);
			
			ASMLdr load = new ASMLdr(new RegOperand(target), new RegOperand(target));
			load.comment = new ASMComment("Load from address");
			ref.instructions.add(load);
		}
		else {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0), new ImmOperand(2));
			lsl.comment = new ASMComment("Convert to bytes");
			ref.instructions.add(lsl);
			
			/* Sequentially push words on stack */
			for (int i = 0; i < a.getType().wordsize(); i++) {
				ASMLdr load = new ASMLdr(new RegOperand(target), new RegOperand(REGISTER.R1), new ImmOperand(i * 4));
				ref.instructions.add(load);
				ref.instructions.add(new ASMPushStack(new RegOperand(target)));
			}
		}
		
		return ref;
	}
	
}
