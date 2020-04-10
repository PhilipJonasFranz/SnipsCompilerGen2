package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.IDRef;

public class AsNAddressOf extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAddressOf cast(AddressOf a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNAddressOf aof = new AsNAddressOf();
		a.castedNode = aof;
		
		aof.clearReg(r, st, 0);
		
		IDRef ref = (IDRef) a.expression;
		
		/* Declaration cannot be loaded in regset since the AST was scanned for addressof-nodes,
		 * and was pushed on the stack. */
		
		if (map.declarationLoaded(ref.origin)) {
			/* Get address from global memory */
			ASMDataLabel label = map.resolve(ref.origin);
			
			ASMLdrLabel load = new ASMLdrLabel(new RegOperand(target), new LabelOperand(label));
			load.comment = new ASMComment("Load data section address");
			aof.instructions.add(load);
		}
		else if (st.getParameterByteOffset(ref.origin) != -1) {
			/* Get address from parameter stack */
			int offset = st.getParameterByteOffset(ref.origin);
			
			/* Get offset of parameter relative to fp */
			aof.instructions.add(new ASMAdd(new RegOperand(target), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
		}
		else {
			/* Get address from local stack */
			int offset = st.getDeclarationInStackByteOffset(ref.origin);
			offset += (ref.origin.type.wordsize() - 1) * 4;
			
			/* Load offset of array in memory */
			aof.instructions.add(new ASMSub(new RegOperand(target), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
		}
		
		/* Convert to words for pointer arithmetic */
		aof.instructions.add(new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		
		return aof;
	}
	
}
