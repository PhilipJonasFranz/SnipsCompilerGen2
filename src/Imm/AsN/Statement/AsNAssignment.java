package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Lhs.ElementSelectLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Statement.Assignment;
import Imm.AsN.Expression.AsNElementSelect;
import Imm.AsN.Expression.AsNElementSelect.SELECT_TYPE;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNAssignment extends AsNStatement {

	public static AsNAssignment cast(Assignment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNAssignment assign = new AsNAssignment();
		
		/* Compute value */
		assign.instructions.addAll(AsNExpression.cast(a.value, r, map, st).getInstructions());
		
		/* Declaration already loaded, just move value into register */
		if (r.declarationLoaded(a.origin)) {
			int reg = r.declarationRegLocation(a.origin);
			assign.instructions.add(new ASMMov(new RegOperand(reg), new RegOperand(0)));
		}
		/* Variable is global variable and type is primitive, store to memory.
		 * 		Other types in memory are handled down below. */
		else if (map.declarationLoaded(a.origin) && a.origin.type instanceof PRIMITIVE) {
			ASMDataLabel label = map.resolve(a.origin);
			
			/* Load memory address */
			assign.instructions.add(new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label)));
				
			/* Store computed to memory */
			assign.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
		}
		/* Store to stack */
		else {
			if (a.origin.type instanceof PRIMITIVE) {
				int off = st.getDeclarationInStackByteOffset(a.origin);
				assign.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.FP), 
					new PatchableImmOperand(PATCH_DIR.DOWN, -off)));
			}
			else if (a.origin.type instanceof ARRAY) {
				if (a.lhsId instanceof ElementSelectLhsId) {
					ElementSelectLhsId lhs = (ElementSelectLhsId) a.lhsId;
					ElementSelect select = lhs.selection;
					
					/* Assign sub Array */
					if (select.type instanceof ARRAY) {
						/* Save to param Stack */
						if (st.getParameterByteOffset(a.origin) != -1) 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.PARAM_SUB, assign, select, r, map, st);
						/* Save to global memory */
						else if (map.resolve(a.origin) != null) 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SUB, assign, select, r, map, st);
						/* Save to local stack */
						else 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.LOCAL_SUB, assign, select, r, map, st);
					
						/* Data is on stack, copy to location */
						assign.copyArray((ARRAY) select.type);
					}
					/* Assign single array cell */
					else {
						/* Push value on the stack */
						assign.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
						
						/* Save to param Stack */
						if (st.getParameterByteOffset(a.origin) != -1) 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.PARAM_SINGLE, assign, select, r, map, st);
						/* Save to global memory */
						else if (map.resolve(a.origin) != null) 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.GLOBAL_SINGLE, assign, select, r, map, st);
						/* Save to local stack */
						else 
							AsNElementSelect.injectAddressLoader(SELECT_TYPE.LOCAL_SINGLE, assign, select, r, map, st);
						
						/* Pop the value off the stack and store it at the target location */
						assign.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
						assign.instructions.add(new ASMStr(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
					}
				}
				else if (a.lhsId instanceof SimpleLhsId) {
					SimpleLhsId lhs = (SimpleLhsId) a.lhsId;
				
					/* Use light variations of the addressing injector from AsNElementSelect, since we 
					 * dont have to add the sum to the sub structure.
					 */
					
					/* Parameter */
					if (st.getParameterByteOffset(a.origin) != -1) {
						int offset = st.getParameterByteOffset(a.origin);
						
						ASMAdd start = new ASMAdd(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new PatchableImmOperand(PATCH_DIR.UP, offset));
						start.comment = new ASMComment("Start of structure in stack");
						assign.instructions.add(start);
					}
					else if (map.resolve(a.origin) != null) {
						ASMDataLabel label = map.resolve(a.origin);
						
						ASMLdrLabel load = new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label));
						load.comment = new ASMComment("Load data section address");
						assign.instructions.add(load);
					}
					/* Local */
					else {
						int offset = st.getDeclarationInStackByteOffset(a.origin);
						offset += (a.origin.type.wordsize() - 1) * 4;
						
						/* Load the start of the structure into R1 */
						ASMSub sub = new ASMSub(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.FP), new ImmOperand(offset));
						sub.comment = new ASMComment("Start of structure in stack");
						assign.instructions.add(sub);
					}
					
					/* Copy array */
					assign.copyArray((ARRAY) lhs.origin.type);
				}
			}
			else {
				// TODO Pointer, Struct...
			}
		}
		
		return assign;
	}
	
	protected void copyArray(ARRAY arr) {
		/* Do it sequentially for 8 or less words to copy */
		if (arr.wordsize() <= 8) {
			int offset = 0;
			for (int a = 0; a < arr.wordsize(); a++) {
				/* Pop data from stack */
				this.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
				
				this.instructions.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(offset)));
				
				offset += 4;
			}
		}
		/* Do it via ASM Loop for bigger data chunks */
		else {
			// TODO: Test this implementatio
			
			/* Move counter in R2 */
			this.instructions.add(new ASMAdd(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(arr.wordsize() * 4)));
			
			ASMLabel loopStart = new ASMLabel(LabelGen.getLabel());
			loopStart.comment = new ASMComment("Copy memory section with loop");
			this.instructions.add(loopStart);
			
			ASMLabel loopEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Check if whole sub array was loaded */
			this.instructions.add(new ASMCmp(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
			/* Branch to loop end */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
			
			/* Pop value from stack and store it at location */
			this.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R0)));
			this.instructions.add(new ASMStrStack(MEM_OP.POST_WRITEBACK, new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new ImmOperand(4)));
			
			/* Branch to loop start */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(loopStart)));
			
			this.instructions.add(loopEnd);
		}
	}
	
}
