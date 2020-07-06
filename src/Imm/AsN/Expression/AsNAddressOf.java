package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMov;
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
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNAddressOf extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAddressOf cast(AddressOf a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNAddressOf aof = new AsNAddressOf();
		a.castedNode = aof;
		
		aof.clearReg(r, st, 0, 1);
		
		if (a.expression instanceof IDRef) {
			IDRef ref = (IDRef) a.expression;
			
			/* Declaration cannot be loaded in regset since the AST was scanned for addressof-nodes,
			 * and was pushed on the stack. */
			
			if (map.declarationLoaded(ref.origin)) {
				/* Get address from global memory */
				ASMDataLabel label = map.resolve(ref.origin);
				
				ASMLdrLabel load = new ASMLdrLabel(new RegOperand(target), new LabelOperand(label), ref.origin);
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
				offset += (ref.origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOperand(target), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			}
		}
		else if (a.expression instanceof ArraySelect) {
			ArraySelect select = (ArraySelect) a.expression;
			
			if (select.getType() instanceof ARRAY)
				AsNArraySelect.loadSumR2(aof, select, r, map, st, true);
			else 
				AsNArraySelect.loadSumR2(aof, select, r, map, st, false);
			
			Declaration origin = select.idRef.origin;
			if (map.declarationLoaded(origin)) {
				/* Get address from global memory */
				ASMDataLabel label = map.resolve(origin);
				
				ASMLdrLabel load = new ASMLdrLabel(new RegOperand(REGISTER.R0), new LabelOperand(label), origin);
				load.comment = new ASMComment("Load data section address");
				aof.instructions.add(load);
			}
			else if (st.getParameterByteOffset(origin) != -1) {
				/* Get address from parameter stack */
				int offset = st.getParameterByteOffset(origin);
				
				/* Get offset of parameter relative to fp */
				aof.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset)));
			}
			else {
				/* Get address from local stack */
				int offset = st.getDeclarationInStackByteOffset(origin);
				offset += (origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new ImmOperand(offset)));
			}
			
			ASMAdd add = new ASMAdd(new RegOperand(target), new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2));
			add.comment = new ASMComment("Add structure offset");
			aof.instructions.add(add);
		}
		else if (a.expression instanceof StructSelect) {
			StructSelect select = (StructSelect) a.expression;
			
			AsNStructSelect.injectAddressLoader(aof, select, r, map, st);
			
			aof.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		
		/* Convert to words for pointer arithmetic */
		aof.instructions.add(new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(2)));
		
		return aof;
	}
	
}
