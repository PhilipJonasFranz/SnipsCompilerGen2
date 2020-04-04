package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.StructureInit;

public class AsNStructureInit extends AsNExpression {

			/* --- METHODS --- */
	public static AsNStructureInit cast(StructureInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNStructureInit init = new AsNStructureInit();
		s.castedNode = init;
		
		r.free(0);
		
		/* Compute all elements, push them push them with dummy value on the stack */
		for (int i = 0; i < s.elements.size(); i++) {
			/* Compute Value */
			init.instructions.addAll(AsNExpression.cast(s.elements.get(i), r, map, st).getInstructions());
			
			/* Push on stack, push R0 on stack, AsNDeclaration will pop the R0s and replace it with the declaration */
			if (!(s.elements.get(i) instanceof StructureInit)) {
				init.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				st.push(REGISTER.R0);
			}
		}
		
		return init;
	}
	
}
