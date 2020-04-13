package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.ArrayInit;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNArrayInit extends AsNExpression {

			/* --- METHODS --- */
	public static AsNArrayInit cast(ArrayInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNArrayInit init = new AsNArrayInit();
		s.castedNode = init;
		
		r.free(0, 1, 2);
		
		/* Compute all elements, push them push them with dummy value on the stack */
		int regs = 0;
		for (int i = s.elements.size() - 1; i >= 0; i--) {
			/* If elements are multiple atoms after another, the push can be grouped together in max three */
			if (s.elements.get(i) instanceof Atom) {
				Atom atom = (Atom) s.elements.get(i);
				
				/* Load atom directley in destination */
				init.instructions.addAll(AsNAtom.cast(atom, r, map, st, regs).getInstructions());
				regs++;
				
				/* If group size is 3, push them on the stack */
				if (regs == 3) {
					flush(regs, init);
					regs = 0;
				}
				
				st.push(REGISTER.R0);
			}
			else {
				/* Flush all atoms to clear regs */
				flush(regs, init);
				regs = 0;
				
				init.instructions.addAll(AsNExpression.cast(s.elements.get(i), r, map, st).getInstructions());
			
				/* Push on stack, push R0 on stack, AsNDeclaration will pop the R0s and replace it with the declaration */
				if (!(s.elements.get(i).type instanceof ARRAY)) {
					init.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
					st.push(REGISTER.R0);
				}
			}
		}
		
		/* Flush remaining atoms */
		flush(regs, init);
		
		return init;
	}
	
	/**
	 * Shared with AsNIDRef.<br>
	 * Flush {@link #regs} on the stack. The flushed regs can be R0, R1, R2, based on regs.
	 * F. e. if regs is 1, only R0 is flushed. If regs equals 3, R2, R1, R0 are flushed.<br>
	 * The push order is so that f.E. R2 would end up at a higher address in the stack than R0.<br>
	 * Requires that regs is between 0 and 3.
	 */
	public static void flush(int regs, AsNNode node) {
		if (regs > 0) {
			if (regs == 3) node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			else if (regs == 2) node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			else node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
		}
	}
	
}
