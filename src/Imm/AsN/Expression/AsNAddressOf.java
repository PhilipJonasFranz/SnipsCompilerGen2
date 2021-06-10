package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.REG;
import Imm.AST.Expression.*;
import Imm.AST.Statement.Declaration;
import Res.Const;

public class AsNAddressOf extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNAddressOf cast(AddressOf a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNAddressOf aof = new AsNAddressOf().pushCreatorStack(a);
		aof.clearReg(r, st, false, 0, 1);

		if (a.expression instanceof IDRef || a.expression instanceof IDRefWriteback) {
			/*
			 * We have to extract the IDRef to determine where it's origin is loaded.
			 */
			IDRef ref;

			if (a.expression instanceof IDRef ref0) ref = ref0;
			else {
				IDRefWriteback idwb = (IDRefWriteback) a.expression;

				/* Cast the writeback operation */
				aof.instructions.addAll(AsNIDRefWriteback.cast(idwb, r, map, st).getInstructions());

				/* Re-wire reference from sub-expression */
				ref = idwb.idRef;
			}

			/*
			 * Declaration cannot be loaded in register since the AST was
			 * scanned for addressof-nodes, and was pushed on the stack.
			 */
			if (map.declarationLoaded(ref.origin)) {
				/* Get address from global memory */
				ASMDataLabel label = map.resolve(ref.origin);

				ASMLdrLabel load = new ASMLdrLabel(new RegOp(target), new LabelOp(label), ref.origin);
				aof.instructions.add(load.com("Load data section address"));
			}
			else if (st.getParameterByteOffset(ref.origin) != -1) {
				/* Get address from parameter stack */
				int offset = st.getParameterByteOffset(ref.origin);

				/* Get offset of parameter relative to fp */
				aof.instructions.add(new ASMAdd(new RegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
			}
			else if (st.getDeclarationInStackByteOffset(ref.origin) != -1) {
				/* Get address from local stack */
				int offset = st.getDeclarationInStackByteOffset(ref.origin);
				offset += (ref.origin.getType().wordsize() - 1) * 4;

				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOp(target), new RegOp(REG.FP), new ImmOp(offset)));
			}
			else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
		}
		else if (a.expression instanceof ArraySelect select) {

			/* Load offset in structure into R2 */
			AsNArraySelect.loadSumR2(aof, select, r, map, st, select.getType().isArray());

			Declaration origin = select.idRef.origin;

			/* Determine base offset and load it into R0 */
			if (map.declarationLoaded(origin)) {
				/* Get address from global memory */
				ASMDataLabel label = map.resolve(origin);

				ASMLdrLabel load = new ASMLdrLabel(new RegOp(REG.R0), new LabelOp(label), origin);
				aof.instructions.add(load.com("Load data section address"));
			}
			else if (st.getParameterByteOffset(origin) != -1) {
				/* Get address from parameter stack */
				int offset = st.getParameterByteOffset(origin);

				/* Get offset of parameter relative to fp */
				aof.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset)));
			}
			else if (st.getDeclarationInStackByteOffset(origin) != -1) {
				/* Get address from local stack */
				int offset = st.getDeclarationInStackByteOffset(origin);
				offset += (origin.getType().wordsize() - 1) * 4;

				/* Load offset of array in memory */
				aof.instructions.add(new ASMSub(new RegOp(REG.R0), new RegOp(REG.FP), new ImmOp(offset)));
			}
			else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);

			/* Address = Base + Offset */
			ASMAdd add = new ASMAdd(new RegOp(target), new RegOp(REG.R0), new RegOp(REG.R2));
			aof.instructions.add(add.com("Add structure offset"));
		}
		else if (a.expression instanceof StructSelect select) {
			AsNStructSelect.injectAddressLoader(aof, select, r, map, st, true);
			aof.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
		else if (a.expression instanceof StructureInit) {
			/* Cast the structure init */
			aof.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());

			if (a.expression.getType().wordsize() > 1 || a.expression.getType().isStruct()) {
				/* Swap R0 dummys with unbound data regs. */
				st.popXWords(a.expression.getType().wordsize());
				st.push(REG.RX, a.expression.getType().wordsize());
			}
			else {
				/* In Reg Set, push value on stack */
				aof.instructions.add(new ASMPushStack(new RegOp(REG.R0)));

				/* Push RX to mark unbound data on stack */
				st.push(REG.RX);
			}

			/* Move SP in R0, since it is head of structure */
			aof.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.SP)));
		}
		else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);

		/* Convert to words for pointer arithmetic */
		aof.instructions.add(new ASMLsr(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));

		r.free(0);
		return aof.popCreatorStack();
	}

} 
