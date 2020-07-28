package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.Expression.AsNArraySelect;
import Imm.AsN.Expression.AsNDeref;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNIDRef;
import Imm.TYPE.COMPOSIT.ARRAY;

public class AsNForEachStatement extends AsNConditionalCompoundStatement {

	public static AsNForEachStatement cast(ForEachStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNForEachStatement f = new AsNForEachStatement();
		a.castedNode = f;
		
		/* Create jump as target for continue statements */
		ASMLabel continueJump = new ASMLabel(LabelGen.getLabel());
		f.continueJump = continueJump;
		
		/* Open new scope for iterator */
		st.openScope(a);
		
		/* Initialize counter */
		f.instructions.addAll(AsNDeclaration.cast(a.counter, r, map, st).getInstructions());
		
		/* Initialize iterator */
		f.instructions.addAll(AsNDeclaration.cast(a.iterator, r, map, st).getInstructions());
		
		/* Open scope for condition, body and increment statement */
		st.openScope(a);
		
		/* Marks the start of the loop */
		ASMLabel forStart = new ASMLabel(LabelGen.getLabel());
		f.instructions.add(forStart);
		
		/* End of the loop */
		ASMLabel forEnd = new ASMLabel(LabelGen.getLabel());
		
		/* Set jump target for break statements */
		f.breakJump = forEnd;
		
		/* Initialize counter */
		f.instructions.addAll(AsNIDRef.cast(a.ref, r, map, st, 0).getInstructions());
		
		
		/* Compare bounds, branch to end if bound reached */
		if (a.shadowRef.getType() instanceof ARRAY) {
			ARRAY arr = (ARRAY) a.shadowRef.getType();
			
			f.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(arr.length)));
			f.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(forEnd)));
		}
		else {
			f.instructions.addAll(AsNExpression.cast(a.range, r, map, st).getInstructions());
			
			f.loadCounter(r, st, a, 1);
			
			f.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
		
		f.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(forEnd)));
		
		
		/* Load value in iterator depending on counter */
		if (r.declarationLoaded(a.iterator)) {
			
			/* In Reg Set */
			int loc = r.declarationRegLocation(a.iterator);
			
			if (a.shadowRef.getType() instanceof ARRAY) 
				f.instructions.addAll(AsNArraySelect.cast(a.select, r, map, st).getInstructions());
			else 
				f.instructions.addAll(AsNDeref.cast(a.shadowRef, r, map, st).getInstructions());
			
			f.instructions.add(new ASMMov(new RegOp(loc), new RegOp(REG.R0)));
		}
		else {
			/* In Stack */
			if (a.shadowRef.getType() instanceof ARRAY) 
				f.instructions.addAll(AsNArraySelect.cast(a.select, r, map, st).getInstructions());
			else 
				f.instructions.addAll(AsNDeref.cast(a.shadowRef, r, map, st).getInstructions());
			
			if (a.iterator.getType().wordsize() == 1) {
				int off = st.getDeclarationInStackByteOffset(a.iterator);
				f.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			}
			else {
				/* TODO: Store selected section at iterator */
			}
		}
		
		
		/* Add body, dont use addBody() because of custom scope handling */
		for (Statement s : a.body) 
			f.loadStatement(a, s, r, map, st);
		
		
		/* Add jump for continue statements to use as target */
		f.instructions.add(continueJump);
		
		/* Free all declarations in scope */
		popDeclarationScope(f, a, r, st, true);
		
		
		/* Increment Counter */
		if (r.declarationLoaded(a.counter)) {
			/* In Reg Set */
			int loc = r.declarationRegLocation(a.counter);
			f.instructions.add(new ASMAdd(new RegOp(loc), new RegOp(loc), new ImmOp(1)));
		}
		else {
			/* On Stack */
			int off = st.getDeclarationInStackByteOffset(a.counter);
		
			f.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
			f.instructions.add(new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R0), new ImmOp(1)));
			f.instructions.add(new ASMStrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(REG.R0), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
		}
		
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(forStart));
		branch.optFlags.add(OPT_FLAG.LOOP_BRANCH);
		f.instructions.add(branch);
		
		
		/* Add loop end */
		f.instructions.add(forEnd);
		
		/* Free counter and iterator */
		f.clearDec(r, st, a.iterator, a);
		f.clearDec(r, st, a.counter, a);
		
		f.freeDecs(r, a);
		return f;
	}
	
	public void clearDec(RegSet r, StackSet st, Declaration d, ForEachStatement f) {
		/* Remove iterator from register or stack */
		if (r.declarationLoaded(d)) {
			int loc = r.declarationRegLocation(d);
			r.getReg(loc).free();
		}
		else {
			int add = st.closeScope(f, true);
			if (add != 0) {
				this.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(add)));
			}
		}
	}
	
	public void loadCounter(RegSet r, StackSet st, ForEachStatement f, int target) {
		/* Increment Counter */
		if (r.declarationLoaded(f.counter)) {
			/* In Reg Set */
			int loc = r.declarationRegLocation(f.counter);
			if (loc != target)
				this.instructions.add(new ASMMov(new RegOp(target), new RegOp(loc)));
		}
		else {
			/* On Stack */
			int off = st.getDeclarationInStackByteOffset(f.counter);
		
			this.instructions.add(new ASMLdrStack(MEM_OP.PRE_NO_WRITEBACK, new RegOp(target), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.DOWN, -off)));
		}
	}
	
} 