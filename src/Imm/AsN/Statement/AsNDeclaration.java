package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;

public class AsNDeclaration extends AsNStatement {

	public static AsNDeclaration cast(Declaration d, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNDeclaration dec = new AsNDeclaration();
		
		/* Load value, either in R0 or on the stack */
		dec.instructions.addAll(AsNExpression.cast(d.value, r, map, st).getInstructions());
		dec.instructions.get(0).comment = new ASMComment("Evaluate Expression");
		
		int free = r.findFree();
		if (free != -1 && d.type.wordsize() == 1) {
			/* Free Register exists and declaration fits into a register */
			dec.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(0)));
			r.getReg(free).setDeclaration(d);
		}
		else {
			if (d.type.wordsize() == 1) {
				/* Push declaration on the stack */
				dec.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.SP), new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
			}
			else {
				/* Pop Elements only, elements are already on the stack */
				st.popXWords(d.type.wordsize());
			}
			
			/* Push the declaration that covers the popped area */
			st.push(d);
			
			r.getReg(0).free();
		}
		
		return dec;
	}
	
}
