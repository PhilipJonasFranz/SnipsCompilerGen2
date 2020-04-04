package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;

public class AsNElementSelect extends AsNExpression {

			/* --- METHODS --- */
	public static AsNElementSelect cast(ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNElementSelect select = new AsNElementSelect();
		s.castedNode = select;
		
		r.free(0, 1);
		
		if (s.selection.size() == 1) {
			/* Load Index and multiply with 4 */
			select.instructions.addAll(AsNExpression.cast(s.selection.get(0), r, map, st).getInstructions());
			select.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new ImmOperand(s.type.wordsize() * 4)));
			select.instructions.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		
			int offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
			
			select.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			
			select.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			
			select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
		}
		else {
			/* TODO */
		}
		
		return select;
	}
	
}
