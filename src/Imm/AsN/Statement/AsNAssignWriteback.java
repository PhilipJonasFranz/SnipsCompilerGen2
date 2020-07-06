package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.ASMLdr;
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
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNIdRef;
import Imm.AsN.Expression.AsNStructSelect;

public class AsNAssignWriteback extends AsNStatement {

	public static AsNAssignWriteback cast(AssignWriteback wb, RegSet r, MemoryMap map, StackSet st, boolean partOfExpression) throws CGEN_EXCEPTION {
		AsNAssignWriteback w = new AsNAssignWriteback();
		
		injectWriteback(w, wb.reference, r, map, st, partOfExpression);
		
		return w;
	}
	
	public static void injectWriteback(AsNNode node, Expression reference, RegSet r, MemoryMap map, StackSet st, boolean partOfExpression) throws CGEN_EXCEPTION {
		if (reference instanceof IDRefWriteback) {
			IDRefWriteback wb = (IDRefWriteback) reference;
			IDRef ref = wb.idRef;
			
			r.free(0, 1, 2);
			
			/* Load value of id ref */
			node.instructions.addAll(AsNIdRef.cast(ref, r, map, st, 0).getInstructions());
			
			/* Write back to source */
			if (r.declarationLoaded(ref.origin)) {
				int reg = r.declarationRegLocation(ref.origin);
				
				/* Apply writeback operation */
				injectWriteback(node, wb.writeback, reg, partOfExpression);
			}
			else {
				/* Apply writeback operation */
				injectWriteback(node, wb.writeback, 1, partOfExpression);
				
				if (map.declarationLoaded(ref.origin)) {
					/* Load value from memory */
					ASMDataLabel label = map.resolve(ref.origin);
					
					/* Load memory address */
					ASMLdrLabel ldr = new ASMLdrLabel(new RegOperand(REGISTER.R2), new LabelOperand(label), ref.origin);
					ldr.comment = new ASMComment("Load from .data section");
					node.instructions.add(ldr);
					
					node.instructions.add(new ASMStr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
				}
				/* Load from Stack */
				else {
					if (st.getParameterByteOffset(ref.origin) != -1) {
						/* 
						 * Variable is parameter in stack, get offset relative 
						 * to Frame Pointer in Stack, Load from Stack 
						 */
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
		else if (reference instanceof StructSelectWriteback) {
			StructSelectWriteback sel = (StructSelectWriteback) reference;
			
			/* Load the address of the target in R1 */
			AsNStructSelect.injectAddressLoader(node, sel.select, r, map, st);
			
			node.instructions.add(new ASMLdr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
			
			/* Apply writeback operation */
			injectWriteback(node, sel.writeback, 0, partOfExpression);
			
			node.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		else throw new SNIPS_EXCEPTION("Assign writeback not implemented for expression " + reference.getClass().getName());
	}
	
	private static void injectWriteback(AsNNode node, WRITEBACK wb, int target, boolean partOfExpression) {
		if (wb == WRITEBACK.INCR) {
			node.instructions.add(new ASMAdd(new RegOperand(target), new RegOperand(REGISTER.R0), new ImmOperand(1)));
		}
		else {
			node.instructions.add(new ASMSub(new RegOperand(target), new RegOperand(REGISTER.R0), new ImmOperand(1)));
		}
		
		/* 
		 * Add opt flag for optimizer, but only if this writeback is part of an expression, meaning that the result
		 * needs to stay in R0. If this assignment is a statement like i++, the value does not have to stay in R0.
		 */
		if (partOfExpression) node.instructions.get(node.instructions.size() - 1).optFlags.add(OPT_FLAG.WRITEBACK);
	}
	
}
