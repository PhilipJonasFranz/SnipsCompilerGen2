package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Stack.ASMMemOp.MEM_OP;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Stack.ASMStrStack;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;

public class AsNDeclaration extends AsNStatement {

	public static AsNDeclaration cast(Declaration d, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNDeclaration dec = new AsNDeclaration();
		
		// TODO Only works for 1 Word Datatypes */
		dec.instructions.addAll(AsNExpression.cast(d.value, r, st).getInstructions());
		
		int free = r.findFree();
		if (free != -1 && d.type.wordSize == 1) {
			/* Free Register exists and declaration fits into a register */
			dec.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(0)));
			r.getReg(free).setDeclaration(d);
		}
		else {
			/* Push declaration on the stack */
			dec.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.SP), new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
			st.push(d);
			r.getReg(0).free();
		}
		
		return dec;
	}
	
}
