package Imm.AsN.Expression;

import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.StructureInit;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNStructureInit extends AsNExpression {

			/* --- METHODS --- */
	public static AsNStructureInit cast(StructureInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNStructureInit init = new AsNStructureInit();
		s.castedNode = init;
		
		r.free(0, 1, 2);
		
		structureInit(init, s.elements, r, map, st);
		
		return init;
	}
	
	/*
	 * Loads the element in reverse order on the stack, so the first element in the list will end up on the top 
	 * of the stack.
	 */
	public static void structureInit(AsNNode node, List<Expression> elements, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Compute all elements, push them push them with dummy value on the stack */
		int regs = 0;
		for (int i = elements.size() - 1; i >= 0; i--) {
			/* If elements are multiple atoms after another, the push can be grouped together in max three */
			if (elements.get(i) instanceof Atom) {
				Atom atom = (Atom) elements.get(i);
				
				/* Load atom directley in destination */
				node.instructions.addAll(AsNAtom.cast(atom, r, map, st, regs).getInstructions());
				regs++;
				
				/* If group size is 3, push them on the stack */
				if (regs == 3) {
					flush(regs, node);
					regs = 0;
				}
				
				st.push(REGISTER.R0);
			}
			else {
				/* Flush all atoms to clear regs */
				flush(regs, node);
				regs = 0;
				
				node.instructions.addAll(AsNExpression.cast(elements.get(i), r, map, st).getInstructions());
			
				/* Push on stack, push R0 on stack, AsNDeclaration will pop the R0s and replace it with the declaration */
				if (!(elements.get(i).getType() instanceof ARRAY || elements.get(i).getType() instanceof STRUCT)) {
					node.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
					st.push(REGISTER.R0);
				}
			}
		}
		
		/* Flush remaining atoms */
		flush(regs, node);
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