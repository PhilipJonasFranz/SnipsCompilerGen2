package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
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

			/* --- METHODS --- */
	public static AsNInstanceOfExpression cast(InstanceofExpression iof, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNInstanceOfExpression s = new AsNInstanceOfExpression();
		iof.castedNode = s;
		
		s.clearReg(r, st, 0);
		
		STRUCT struct = (STRUCT) iof.instanceType;
		
		if (iof.expression.getType().wordsize() > 1) s.instructions.add(new ASMMov(new RegOp(REG.R10), new RegOp(REG.SP)));
		
		s.instructions.addAll(AsNExpression.cast(iof.expression, r, map, st).getInstructions());
		
		TYPE t = iof.expression.getType();
		while (t instanceof POINTER) {
			s.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
			s.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
			t = ((POINTER) t).targetType;
		}
		
		if (iof.expression.getType().wordsize() > 1) {
			s.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.SP)));
		}
		
		s.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(struct.typedef.SID)));
		s.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.NE)));
		
		if (iof.expression.getType().wordsize() > 1) s.instructions.add(new ASMMov(new RegOp(REG.SP), new RegOp(REG.R10)));
		
		return s;
	}
	
}
