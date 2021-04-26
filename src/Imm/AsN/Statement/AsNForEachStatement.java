package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.AsNBody;
import Imm.AsN.Expression.AsNArraySelect;
import Imm.AsN.Expression.AsNDeref;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNIDRef;
import Imm.AsN.Expression.AsNStructSelect;
import Imm.TYPE.COMPOSIT.ARRAY;
import Opt.AST.Util.Matcher;
import Res.Const;

public class AsNForEachStatement extends AsNConditionalCompoundStatement {

	public static AsNForEachStatement cast(ForEachStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNForEachStatement f = new AsNForEachStatement();
		a.castedNode = f;
		
		/* Create jump as target for continue statements */
		ASMLabel continueJump = new ASMLabel(LabelUtil.getLabel());
		f.continueJump = continueJump;
		
		/* Open new scope for counter and iterator */
		st.openScope(a);
		
		/* Initialize counter */
		f.instructions.addAll(AsNDeclaration.cast(a.counter, r, map, st).getInstructions());
		
		/* Initialize iterator */
		f.instructions.addAll(AsNDeclaration.cast(a.iterator, r, map, st).getInstructions());
		
		if (r.declarationLoaded(a.iterator)) {
			/* Check if an address reference was made to the declaration, if yes, push it on the stack. */
			boolean push = false;
			for (Statement s : a.body)
				push |= Matcher.hasAddressReference(s, a.iterator);
			
			if (push) {
				int reg = r.declarationRegLocation(a.iterator);
				
				f.instructions.add(new ASMPushStack(new RegOp(reg)));
				
				st.push(a.iterator);
				r.free(reg);
			}
		}
		
		/* Open scope for condition, body and increment statement */
		st.openScope(a);
		
		/* Marks the start of the loop */
		ASMLabel forStart = new ASMLabel(LabelUtil.getLabel());
		forStart.optFlags.add(OPT_FLAG.LOOP_HEAD);
		f.instructions.add(forStart);
		
		/* End of the loop */
		ASMLabel forEnd = new ASMLabel(LabelUtil.getLabel());
		
		/* Set jump target for break statements */
		f.breakJump = forEnd;
		
		
		/* Compare bounds, branch to end if bound reached */
		if (a.select != null) {
			/* Load counter */
			f.instructions.addAll(AsNIDRef.cast(a.counterRef, r, map, st, 0).getInstructions());
			
			ARRAY arr = (ARRAY) a.shadowRef.getType();
			
			f.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(arr.getLength())));
		}
		else {
			/* Load the range, automatically calculates range * iterator word size */
			f.instructions.addAll(AsNExpression.cast(a.range, r, map, st).getInstructions());
			
			/* Load counter */
			f.instructions.addAll(AsNIDRef.cast(a.counterRef, r, map, st, 1).getInstructions());
			
			f.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
		
		f.instructions.add(new ASMBranch(BRANCH_TYPE.B, COND.EQ, new LabelOp(forEnd)));
		
		/* Free the counter from the reg set */
		r.free(0);
		
		/* Load value in iterator depending on counter */
		if (r.declarationLoaded(a.iterator)) {
			/* In Reg Set */
			int loc = r.declarationRegLocation(a.iterator);
			
			if (a.select != null) {
				if (a.select.getShadowRef() instanceof StructSelect) {
					
					StructSelect sel = (StructSelect) a.select.getShadowRef();

					AsNStructSelect.injectAddressLoader(f, sel, r, map, st, false);
					
					/* Load counter */
					f.instructions.addAll(AsNIDRef.cast(a.counterRef, r, map, st, 0).getInstructions());
					
					/* Multiply counter with word size */
					if (a.counter.getType().wordsize() > 1) {
						f.instructions.add(new ASMMov(new RegOp(REG.R2), new ImmOp(a.counter.getType().wordsize())));
						f.instructions.add(new ASMMult(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R2)));
					}
					
					f.instructions.add(new ASMLsl(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(2)));
					
					/** Counter offset to absolute address, final address now in R1 */
					f.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R0)));
					
					/* Load value */
					f.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R1)));
				}
				else 
					f.instructions.addAll(AsNArraySelect.cast(a.select, r, map, st).getInstructions());
			}
			else 
				f.instructions.addAll(AsNDeref.cast(a.shadowRef, r, map, st).getInstructions());
			
			/* Move value to location of iterator */
			f.instructions.add(new ASMMov(new RegOp(loc), new RegOp(REG.R0)));
		}
		else {
			/* In Stack */
			if (a.select != null) 
				f.instructions.addAll(AsNArraySelect.cast(a.select, r, map, st).getInstructions());
			else 
				f.instructions.addAll(AsNDeref.cast(a.shadowRef, r, map, st).getInstructions());
			
			if (a.iterator.getType().wordsize() == 1) {
				int off = st.getDeclarationInStackByteOffset(a.iterator);
				f.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			}
			else if (st.getDeclarationInStackByteOffset(a.iterator) != -1) {
				int offset = st.getDeclarationInStackByteOffset(a.iterator);
				offset += (a.iterator.getType().wordsize() - 1) * 4;
				
				/* Load the start of the structure into R1 */
				f.instructions.add(new ASMSub(new RegOp(REG.R1), new RegOp(REG.FP), new ImmOp(offset)));
				
				/* Pop the loaded words and store them to the iterator */
				StackUtil.copyToAddressFromStack(a.iterator.getType().wordsize(), f, st);
			}
			else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
		}
		
		/* Add body, dont use addBody() because of custom scope handling */
		for (Statement s : a.body) 
			f.loadStatement(a, s, r, map, st);
		
		
		/* Add jump for continue statements to use as target */
		f.instructions.add(continueJump);
		
		
		if (a.writeBackIterator) 
			/* Write back the value of the iterator into the base */
			f.instructions.addAll(AsNAssignment.cast(a.writeback, r, map, st).getInstructions());
		
		
		/* Free all declarations in scope */
		popDeclarationScope(f, a, r, st, true);
		
		
		/* Operand will hold the value to increment the counter with */
		Operand incrOp = new ImmOp(1);
		
		/* 
		 * If the shadow ref is a deref, the counter incrmenets by the word size of the iterator,
		 * and thus we need to load the word size for the counter increment.
		 */
		boolean isDeref = a.shadowRef instanceof Deref;
		if (isDeref) {
			AsNBody.literalManager.loadValue(f, a.iterator.getType().wordsize(), 0);
			incrOp = new RegOp(REG.R0);
		}
		
		if (r.declarationLoaded(a.counter)) {
			/* In Reg Set */
			int loc = r.declarationRegLocation(a.counter);
			f.instructions.add(new ASMAdd(new RegOp(loc), new RegOp(loc), incrOp));
		}
		else {
			/* On Stack */
			int off = st.getDeclarationInStackByteOffset(a.counter);
		
			/* Load counter, increment and store */
			f.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			f.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), incrOp));
			f.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
		}
		
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(forStart));
		branch.optFlags.add(OPT_FLAG.LOOP_BRANCH);
		f.instructions.add(branch);
		
		
		/* Add loop end */
		f.instructions.add(forEnd);
		
		/* Remove iterator from reg set */
		if (r.declarationLoaded(a.iterator)) {
			int loc = r.declarationRegLocation(a.iterator);
			r.getReg(loc).free();
		}
		
		/* Remove counter from reg set */
		if (r.declarationLoaded(a.counter)) {
			int loc = r.declarationRegLocation(a.counter);
			r.getReg(loc).free();
		}
		
		/* Reset stack from iterator and counter */
		int add = st.closeScope(a, true);
		if (add != 0) {
			ASMAdd add0 = new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(add));
			add0.comment = new ASMComment("Reset stack, remove iterator and counter");
			f.instructions.add(add0);
		}
		
		f.freeDecs(r, a);
		return f;
	}
	
} 
