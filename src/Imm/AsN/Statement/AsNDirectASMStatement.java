package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMHardcode;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Statement.DirectASMStatement;
import Imm.AsN.Expression.AsNExpression;
import Util.Pair;

public class AsNDirectASMStatement extends AsNStatement {

	public static AsNDirectASMStatement cast(DirectASMStatement d, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNDirectASMStatement asm = new AsNDirectASMStatement();
		d.castedNode = asm;
		
		/* Load data in */
		for (Pair<Expression, REG> p : d.dataIn) {
			asm.instructions.addAll(AsNExpression.cast(p.first, r, map, st).getInstructions());
			asm.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
		}
		
		/* Pop data in in correct regs */
		for (int i = d.dataIn.size() - 1; i >= 0; i--) 
			asm.instructions.add(new ASMPopStack(new RegOp(d.dataIn.get(i).second)));
		
		/* Add hardcoded assembly */
		for (String s : d.assembly) 
			asm.instructions.add(new ASMHardcode(s));
		
		/* Pop data in in correct regs */
		for (Pair<Expression, REG> p : d.dataOut) {
			IDRef ref = (IDRef) p.first;
			
			if (r.declarationLoaded(ref.origin)) {
				int loc = r.declarationRegLocation(ref.origin);
				asm.instructions.add(new ASMMov(new RegOp(loc), new RegOp(p.second)));
			}
			/* Variable is global variable and type is primitive, store to memory.
			 * 		Other types in memory are handled down below. */
			else if (map.declarationLoaded(ref.origin)) {
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load memory address */
				asm.instructions.add(new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), ref.origin));
					
				/* Store computed to memory */
				asm.instructions.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1)));
			}
			/* Store to stack */
			else {
				int off = st.getDeclarationInStackByteOffset(ref.origin);
				
				r.print();
				st.print();
				
				if (off == -1) throw new SNIPS_EXC(ref.path + " is not loaded!");
				
				asm.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			}
		}
		
		return asm;
	}
	
} 
