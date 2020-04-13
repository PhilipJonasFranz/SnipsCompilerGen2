package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.IDRefWriteback.ID_WRITEBACK;
import Imm.AST.Statement.AssignWriteback;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNIdRef;

public class AsNAssignWriteback extends AsNStatement {

	public static AsNAssignWriteback cast(AssignWriteback wb, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNAssignWriteback w = new AsNAssignWriteback();
		
		injectWriteback(w, wb.idWb, r, map, st);
		
		return w;
	}
	
	public static void injectWriteback(AsNNode node, IDRefWriteback wb, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		IDRef ref = wb.idRef;
		
		r.free(0, 1, 2);
		
		/* Load value of id ref */
		node.instructions.addAll(AsNIdRef.cast(ref, r, map, st, 0).getInstructions());
		
		/* Write back to source */
		if (r.declarationLoaded(ref.origin)) {
			int reg = r.declarationRegLocation(ref.origin);
			
			/* Apply operator */
			ASMInstruction ins = null;
			if (wb.idWb == ID_WRITEBACK.INCR) 
				ins = new ASMAdd(new RegOperand(reg), new RegOperand(REGISTER.R0), new ImmOperand(1));
			else 
				ins = new ASMSub(new RegOperand(reg), new RegOperand(REGISTER.R0), new ImmOperand(1));
			
			/* Set flag for optimizer */
			ins.optFlags.put(OPT_FLAG.WRITEBACK, true);
			node.instructions.add(ins);
		}
		else {
			/* Apply operator */
			ASMInstruction ins = null;
			if (wb.idWb == ID_WRITEBACK.INCR) 
				ins = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0), new ImmOperand(1));
			else 
				ins = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0), new ImmOperand(1));
			
			/* Set flag for optimizer */
			ins.optFlags.put(OPT_FLAG.WRITEBACK, true);
			node.instructions.add(ins);	
			
			if (map.declarationLoaded(ref.origin)) {
				/* Load value from memory */
				
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load memory address */
				ins = new ASMLdrLabel(new RegOperand(REGISTER.R2), new LabelOperand(label));
				ins.comment = new ASMComment("Load from .data section");
				node.instructions.add(ins);
				
				node.instructions.add(new ASMStr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			}
			/* Load from Stack */
			else {
				if (st.getParameterByteOffset(ref.origin) != -1) {
					/* Variable is parameter in stack, get offset relative to Frame Pointer in Stack, 
					 * 		Load from Stack */
					int off = st.getParameterByteOffset(ref.origin);
					node.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, off)));
				}
				else {
					/* Load Declaration Location from Stack */
					int off = st.getDeclarationInStackByteOffset(ref.origin);
					node.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
				}
			}
		}
	}
	
}
