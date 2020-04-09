package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.ElementSelect;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNDeref extends AsNExpression {

			/* --- METHODS --- */
	public static AsNDeref cast(Deref a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNDeref ref = new AsNDeref();
		a.castedNode = ref;
		
		ref.clearReg(r, st, 0);
		
		/* Load the Expression. The result will either be loaded directley if its an element select,
		 * or the resulting address the pointer is pointing will end up in R0, and will be loaded down below.
		 */
		ref.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
		
		if (!(a.expression instanceof ElementSelect)) {
			/* Convert to bytes */
			ref.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
			
			if (a.type.wordsize() == 1) {
				ref.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			}
			else if (a.type instanceof ARRAY) {
				ARRAY arr = (ARRAY) a.type;
				
				int offset = (arr.getLength() - 1) * 4;
				
				/* Copy memory location with the size of the array */
				int regs = 1;
				for (int i = 0; i < arr.wordsize(); i++) {
					if (regs < 3) {
						ref.instructions.add(new ASMLdr(new RegOperand(regs), new RegOperand(REGISTER.R0), new ImmOperand(offset)));
						regs++;
					}
					if (regs == 3) {
						ref.flush(regs);
						regs = 1;
					}
					offset -= 4;
					st.push(REGISTER.R0);
				}
				
				ref.flush(regs);
			}
		}
		
		return ref;
	}
	
	/**
	 * Assumes that R0 is occupied by the base pointer address
	 * @param regs
	 */
	public void flush(int regs) {
		if (regs == 3) this.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1)));
		else if (regs == 2) this.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R1)));
	}
	
}
