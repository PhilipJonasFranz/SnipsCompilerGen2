package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMHardcode;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Statement.DirectASMStatement;
import Imm.AsN.Expression.AsNExpression;
import Util.Pair;

public class AsNDirectASMStatement extends AsNStatement {

	public static AsNDirectASMStatement cast(DirectASMStatement d, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNDirectASMStatement asm = new AsNDirectASMStatement();
		d.castedNode = asm;
		
		/* Load data in */
		for (Pair<Expression, REGISTER> p : d.dataIn) {
			asm.instructions.addAll(AsNExpression.cast(p.first, r, map, st).getInstructions());
			asm.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
		}
		
		/* Pop data in in correct regs */
		for (int i = d.dataIn.size() - 1; i >= 0; i--) {
			asm.instructions.add(new ASMPopStack(new RegOperand(d.dataIn.get(i).second)));
		}
		
		/* Add hardcoded assembly */
		for (String s : d.assembly) {
			asm.instructions.add(new ASMHardcode(s));
		}
		
		/* Pop data in in correct regs */
		for (Pair<Expression, REGISTER> p : d.dataOut) {
			IDRef ref = (IDRef) p.first;
			
			if (r.declarationLoaded(ref.origin)) {
				int loc = r.declarationRegLocation(ref.origin);
				asm.instructions.add(new ASMMov(new RegOperand(loc), new RegOperand(p.second)));
			}
			/* Variable is global variable and type is primitive, store to memory.
			 * 		Other types in memory are handled down below. */
			else if (map.declarationLoaded(ref.origin)) {
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load memory address */
				asm.instructions.add(new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label), ref.origin));
					
				/* Store computed to memory */
				asm.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			}
			/* Store to stack */
			else {
				int off = st.getDeclarationInStackByteOffset(ref.origin);
					
				asm.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
			}
		}
		
		return asm;
	}
	
}
