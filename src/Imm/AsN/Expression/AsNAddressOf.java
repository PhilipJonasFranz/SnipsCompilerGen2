package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNAddressOf extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAddressOf cast(AddressOf a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
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
				
				ASMLdrLabel load = new ASMLdrLabel(new RegOp(target), new LabelOp(label), ref.origin);
				load.comment = new ASMComment("Load data section address");
				aof.instructions.add(load);
			}
			else if (st.getParameterByteOffset(ref.origin) != -1) {
				/* Get address from parameter stack */
				int offset = st.getParameterByteOffset(ref.origin);
				
				/* Get offset of parameter relative to fp */
				aof.instructions.add(new ASMAdd(new RegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
			}
			else {
				/* Get address from local stack */
				int offset = st.getDeclarationInStackByteOffset(ref.origin);
				offset += (ref.origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOp(target), new RegOp(REG.FP), new ImmOp(offset)));
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
				
				ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R0), new LabelOp(label), origin);
				load.comment = new ASMComment("Load data section address");
				aof.instructions.add(load);
			}
			else if (st.getParameterByteOffset(origin) != -1) {
				/* Get address from parameter stack */
				int offset = st.getParameterByteOffset(origin);
				
				/* Get offset of parameter relative to fp */
				aof.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
			}
			else {
				/* Get address from local stack */
				int offset = st.getDeclarationInStackByteOffset(origin);
				offset += (origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOp(REG.R0), new RegOp(REG.FP), new ImmOp(offset)));
			}
			
			ASMAdd add = new ASMAdd(new RegOp(target), new RegOp(REG.R0), new RegOp(REG.R2));
			add.comment = new ASMComment("Add structure offset");
			aof.instructions.add(add);
		}
		else if (a.expression instanceof StructSelect) {
			StructSelect select = (StructSelect) a.expression;
			
			AsNStructSelect.injectAddressLoader(aof, select, r, map, st, true);
			
			aof.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
		else if (a.expression instanceof StructureInit) {
			/* Cast the structure init */
			aof.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
			
			if (a.expression.getType().wordsize() > 1 || a.expression.getType() instanceof STRUCT) {
				/* Swap R0 dummys with unbound data regs. */
				st.popXWords(a.expression.getType().wordsize());
				for (int i = 0; i < a.expression.getType().wordsize(); i++) st.push(REG.RX);
				
				/* Move SP in R0, since it is head of structure */
				aof.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.SP)));
			}
			else {
				/* In Reg Set, push value on stack */
				aof.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				
				/* Push RX to mark unbound data on stack */
				st.push(REG.RX);
				
				/* Move SP in R0, since it is head of structure */
				aof.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.SP)));
			}
		}
		
		/* Convert to words for pointer arithmetic */
		aof.instructions.add(new ASMLsr(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
		
		return aof;
	}
	
} 
