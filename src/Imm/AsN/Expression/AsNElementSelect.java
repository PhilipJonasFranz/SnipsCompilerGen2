package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.Expression;

public class AsNElementSelect extends AsNExpression {

			/* --- METHODS --- */
	public static AsNElementSelect cast(ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNElementSelect select = new AsNElementSelect();
		s.castedNode = select;
		
		r.free(0, 1);
		
		/* Array is parameter, load from parameter stack */
		if (st.getParameterByteOffset(s.idRef.origin) != -1) {
			select.loadIndex(s, s.selection.get(0), r, map, st);
			
			int offset = st.getParameterByteOffset(s.idRef.origin);
			
			/* Offset to start of array */
			offset += (s.idRef.origin.type.wordsize() - 1) * 4;
			
			/* R1 is now memory location of first element of array */
			select.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
			
			/* Subtract index byte offset to final memory location */
			select.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
			
			/* Load */
			select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
		}
		else {
			/* Load from local stack */
			if (s.selection.size() == 1) {
				/* Only one selection */
				select.loadIndex(s, s.selection.get(0), r, map, st);
				
				int offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
				
				select.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
				
				select.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
				
				select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
			}
			else {
				/* TODO */
				/* Multiple Selections */
			}
		}
		
		return select;
	}
	
	protected void loadIndex(ElementSelect s, Expression indexExpression, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Load Index and multiply with 4 to convert index to byte offset */
		this.instructions.addAll(AsNExpression.cast(indexExpression, r, map, st).getInstructions());
		this.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
	}
	
}
