package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.InstanceofExpression;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNInstanceOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNInstanceOfExpression cast(InstanceofExpression iof, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNInstanceOfExpression s = new AsNInstanceOfExpression();
		iof.castedNode = s;
		
		STRUCT struct = (STRUCT) iof.instanceType;
		
		r.free(0, 1);
		
		/* 
		 * Get size of stack in cells, so that if casting the expression puts something on the stack,
		 * we know how much cells we have to remove in order to reset the stack.
		 */
		int stackSize = st.getStack().size();
		
		/* Cast the expression */
		s.instructions.addAll(AsNExpression.cast(iof.expression, r, map, st).getInstructions());
		
		/* 
		 * Deref as long as the expression is a pointer, at one point the core value is reached, and ends
		 * up in R0 or on the stack.
		 */
		TYPE t = iof.expression.getType();
		while (t instanceof POINTER) {
			s.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
			s.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
			t = ((POINTER) t).targetType;
		}
		
		/* Expression ended up on the stack, load first word in R0 */
		if (iof.expression.getType().wordsize() > 1) 
			s.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.SP)));
		
		/* No structs extended from this struct or SID is highest, a simple SID check is enough */
		if (struct.getTypedef().extenders.isEmpty()) {
			/* Compare value in R0 agains SID */
			s.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(struct.getTypedef().SID)));
			
			/* Move 0 if check is false, in the other case a non-zero value is already in R0 */
			s.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.NE)));
		}
		else {
			if (struct.getTypedef().SIDNeighbour == null) {
				/* Compare value in R0 agains SID */
				s.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(struct.getTypedef().SID)));
				
				/* Move 0 if check is false, in the other case a non-zero value is already in R0 */
				s.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.LT)));
			}
			else {
				/* Compare the lower SID range end */
				s.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(struct.getTypedef().SID)));
				
				/* Increment counter if matched greater or equal */
				s.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.LT)));
				
				/* Compare the upper SID range end */
				s.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(struct.getTypedef().SIDNeighbour.SID)));
				
				/* Increment counter if matched greater or equal */
				s.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.GE)));
			}
		}

		/* Only reset stack if expressions were loaded on the stack, meaning the core type wordsize is > 1 */
		if (iof.expression.getType().getCoreType().wordsize() > 1) {
			int off = st.popXCells(st.getStack().size() - stackSize);
			s.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(off)));
		}
		
		r.free(0);
		return s;
	}
	
} 
