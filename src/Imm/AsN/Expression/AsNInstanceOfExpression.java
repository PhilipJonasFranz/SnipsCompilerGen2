package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.InstanceofExpression;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNInstanceOfExpression extends AsNExpression {

			/* --- METHODS --- */
	public static AsNInstanceOfExpression cast(InstanceofExpression iof, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNInstanceOfExpression s = new AsNInstanceOfExpression();
		iof.castedNode = s;
		
		s.clearReg(r, st, 0);
		
		STRUCT struct = (STRUCT) iof.instanceType;
		
		if (iof.expression.getType().wordsize() > 1) s.instructions.add(new ASMMov(new RegOperand(REGISTER.R10), new RegOperand(REGISTER.SP)));
		
		s.instructions.addAll(AsNExpression.cast(iof.expression, r, map, st).getInstructions());
		
		TYPE t = iof.expression.getType();
		while (t instanceof POINTER) {
			s.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
			s.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			t = ((POINTER) t).targetType;
		}
		
		if (iof.expression.getType().wordsize() > 1) {
			s.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.SP)));
		}
		
		s.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(struct.typedef.SID)));
		s.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.NE)));
		
		if (iof.expression.getType().wordsize() > 1) s.instructions.add(new ASMMov(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.R10)));
		
		return s;
	}
	
}
