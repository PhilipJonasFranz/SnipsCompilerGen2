package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.Expression;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNElementSelect extends AsNExpression {

			/* --- METHODS --- */
	public static AsNElementSelect cast(ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNElementSelect select = new AsNElementSelect();
		s.castedNode = select;
		
		r.free(0, 1, 2);
		
		/* Array is parameter, load from parameter stack */
		if (st.getParameterByteOffset(s.idRef.origin) != -1) {
			if (s.type instanceof ARRAY) {
				// TODO -> ERROR Array\10
				/* Load part of array that is a parameter */
			}
			else {
				if (s.selection.size() == 1) {
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
					// TODO
					select.loadSumR2(s, r, map, st);
					
					/* Load last selection */
					select.loadIndex(s, s.selection.get(s.selection.size() - 1), r, map, st);
					
					int offset = st.getParameterByteOffset(s.idRef.origin);
					
					/* Offset to start of array */
					offset += (s.idRef.origin.type.wordsize() - 1) * 4;
					
					select.instructions.add(new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
					
					/* Subtract sum */
					select.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
					
					/* Subtract last selection */
					select.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
					
					/* Load */
					select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
				}
			}
		}
		else {
			if (s.type instanceof ARRAY) {
				ARRAY arr = (ARRAY) s.type;

				/* Load block offset */
				ASMMov sum = new ASMMov(new RegOperand(REGISTER.R2), new ImmOperand(0));
				sum.comment = new ASMComment("Calculate save location");
				select.instructions.add(sum);
				
				ARRAY superType = (ARRAY) s.idRef.origin.type;
				
				/* Handle selections differently, since last selection does not result in primitive. 
				 * 		This algorithm is a custom variation of the loadSumR2 method. */
				for (int i = 0; i < s.selection.size(); i++) {
					/* Evaluate Expression */
					select.instructions.addAll(AsNExpression.cast(s.selection.get(i), r, map, st).getInstructions());
					
					int bytes = superType.elementType.wordsize() * 4;
					
					select.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new ImmOperand(bytes)));
					select.instructions.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
					
					/* Add to sum */
					select.instructions.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R0)));
					
					/* Next element in chain */
					if (!(superType.elementType instanceof ARRAY)) break;
					else superType = (ARRAY) superType.elementType;
				}
				
				/* Load the start of the structure into R1 */
				int offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
				ASMSub sub = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset));
				sub.comment = new ASMComment("Start of structure in stack");
				select.instructions.add(sub);
			
				/* Sub the offset to the start of the sub structure from the start in R1 */
				ASMSub block = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2));
				block.comment = new ASMComment("Start of sub structure in stack");
				select.instructions.add(block);
				
				offset = 0;
				
				/* Copy memory location with the size of the array */
				for (int a = 0; a < arr.getLength(); a++) {
					select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
					select.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
					offset -= 4;
				}
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
					/* Multiple Selections */
					
					select.loadSumR2(s, r, map, st);
					
					/* Load offset of array in memory */
					int offset = st.getDeclarationInStackByteOffset(s.idRef.origin);
					select.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
					
					/* Location - block offset */
					select.instructions.add(new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
					
					/* Load last selection */
					select.instructions.addAll(AsNExpression.cast(s.selection.get(s.selection.size() - 1), r, map, st).getInstructions());
					/* Convert to bytes */
					select.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
					
					/* Final address */
					select.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
					
					/* Load */
					select.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0)));
				}
			}
		}
		
		return select;
	}
	
	protected void loadIndex(ElementSelect s, Expression indexExpression, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Load Index and multiply with 4 to convert index to byte offset */
		this.instructions.addAll(AsNExpression.cast(indexExpression, r, map, st).getInstructions());
		this.instructions.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
	}
	
	protected void loadSumR2(ElementSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Sum */
		ASMMov sum = new ASMMov(new RegOperand(REGISTER.R2), new ImmOperand(0));
		sum.comment = new ASMComment("Calculate save location");
		this.instructions.add(sum);
		
		ARRAY superType = (ARRAY) s.idRef.origin.type;
		
		for (int i = 0; i < s.selection.size() - 1; i++) {
			/* Evaluate Expression */
			this.instructions.addAll(AsNExpression.cast(s.selection.get(i), r, map, st).getInstructions());
			
			int bytes = superType.elementType.wordsize() * 4;
			
			/* Next element in chain */
			if (i < s.selection.size() - 1) superType = (ARRAY) superType.elementType;
			
			this.instructions.add(new ASMMov(new RegOperand(REGISTER.R1), new ImmOperand(bytes)));
			this.instructions.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			
			/* Add to sum */
			this.instructions.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R0)));
		}
	}
	
}
