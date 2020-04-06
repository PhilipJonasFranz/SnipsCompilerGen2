package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.StructureInit;

public class AsNStructureInit extends AsNExpression {

			/* --- METHODS --- */
	public static AsNStructureInit cast(StructureInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNStructureInit init = new AsNStructureInit();
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
					init.flush(regs);
					regs = 0;
				}
				
				st.push(REGISTER.R0);
			}
			else {
				/* Flush all atoms to clear regs */
				init.flush(regs);
				regs = 0;
				
				init.instructions.addAll(AsNExpression.cast(s.elements.get(i), r, map, st).getInstructions());
			
				/* Push on stack, push R0 on stack, AsNDeclaration will pop the R0s and replace it with the declaration */
				if (!(s.elements.get(i) instanceof StructureInit)) {
					init.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
					st.push(REGISTER.R0);
				}
			}
		}
		
		/* Flush remaining atoms */
		init.flush(regs);
		
		return init;
	}
	
	protected void flush(int regs) {
		if (regs > 0) {
			if (regs == 3) this.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			else if (regs == 2) this.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			else this.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
		}
	}
	
}
