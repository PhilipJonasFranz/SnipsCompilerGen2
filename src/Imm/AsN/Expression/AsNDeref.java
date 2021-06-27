package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Memory.ASMVLdr;
import Imm.AST.Expression.Deref;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNDeref extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNDeref cast(Deref a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNDeref ref = new AsNDeref().pushCreatorStack(a);

		boolean isVFP = a.getType().isFloat();

		ref.clearReg(r, st, isVFP, 0, 1);

		/* Load Expression */
		ref.instructions.addAll(AsNExpression.cast(a.expression, r, map, st).getInstructions());
		
		/* Load from memory, load into R0 */
		if (a.getType().wordsize() == 1) {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOp(target), new RegOp(REG.R0), new ImmOp(2));
			ref.instructions.add(lsl.com("Convert to bytes"));

			if (isVFP) {
				ASMVLdr load = new ASMVLdr(new VRegOp(target), new RegOp(target));
				ref.instructions.add(load.com("Load from address"));
			}
			else {
				ASMLdr load = new ASMLdr(new RegOp(target), new RegOp(target));
				ref.instructions.add(load.com("Load from address"));
			}
		}
		/* Load on stack */
		else {
			/* Convert to bytes */
			ASMLsl lsl = new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R0), new ImmOp(2));
			ref.instructions.add(lsl.com("Convert to bytes"));

			/* Sequentially push words on stack */
			for (int i = 0; i < a.getType().wordsize(); i++) {
				/*
				 * If this is the last dataword that is loaded, or this is the dataword that would be the SID of a struct that is pushed on
				 * the stack, override the data and push the registered SID of the struct. This is due to polymorphism, that a different SID
				 * could be stored at this location. Since we load the struct, polymorphism is not available anymore, and thus we need to overwrite
				 * the stored SID with the actual SID of the struct type.
				 */
				if (i == a.getType().wordsize() - 1 && a.getType().getCoreType().isStruct()) {
					STRUCT s = (STRUCT) a.getType().getCoreType();
					s.getTypedef().loadSIDInReg(ref, REG.toReg(target), s.proviso);
				}
				else {
					ASMLdr load = new ASMLdr(new RegOp(target), new RegOp(REG.R1), new ImmOp((a.getType().wordsize() - i - 1) * 4));
					ref.instructions.add(load);
				}
				
				ref.instructions.add(new ASMPushStack(new RegOp(target)));
				
				/* Push dummy values on stack */
				st.pushDummy();
			}
		}

		return ref.popCreatorStack();
	}
	
} 
