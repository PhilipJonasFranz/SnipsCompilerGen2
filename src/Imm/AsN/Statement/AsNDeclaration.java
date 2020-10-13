package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNDeclaration extends AsNStatement {

	public static AsNDeclaration cast(Declaration d, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNDeclaration dec = new AsNDeclaration();
		
		/* Load value, either in R0 or on the stack */
		if (d.value != null) dec.instructions.addAll(AsNExpression.cast(d.value, r, map, st).getInstructions());
		if (!dec.instructions.isEmpty()) dec.instructions.get(0).comment = new ASMComment("Evaluate Expression");
		
		int free = r.findFree();
		if (free != -1 && (d.getType() instanceof PRIMITIVE || d.getType() instanceof POINTER || d.getType() instanceof INTERFACE)) {
			/* Free Register exists and declaration fits into a register */
			ASMMov mov = new ASMMov(new RegOp(free), new RegOp(0));
			mov.optFlags.add(OPT_FLAG.WRITEBACK);
			dec.instructions.add(mov);
			
			r.getReg(free).setDeclaration(d);
		}
		else {
			/* Push only if primitive or pointer, in every other case the expression 
			 * is already on the stack */
			if ((d.getType() instanceof PRIMITIVE || d.getType() instanceof POINTER || d.getType() instanceof INTERFACE) && !(d.value instanceof StructureInit)) {
				dec.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
			}
			else {
				/* Pop R0 placeholders pushed by structure init from stack set, but dont add the assembly code 
				 * to do so, this is so that below the correct declaration can be pushed and lines up with 
				 * the values on the stack */
				if (d.value != null) st.popXWords(d.getType().wordsize());
				else {
					/* Declaration has no value, just make space on the stack */
					ASMSub sub = new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(d.getType().wordsize() * 4));
					sub.comment = new ASMComment("Make space on stack for declaration " + d.path.getLast());
					
					dec.instructions.add(sub);
				}
			}
			
			/* Push the declaration that covers the popped area */
			st.push(d);
			r.getReg(0).free();
		}
		
		dec.freeDecs(r, d);
		return dec;
	}
	
} 
