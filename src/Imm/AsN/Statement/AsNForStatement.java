package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCompare;
import Opt.AST.Util.Matcher;

public class AsNForStatement extends AsNConditionalCompoundStatement {

	public static AsNForStatement cast(ForStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNForStatement f = new AsNForStatement();
		f.pushOnCreatorStack(a);
		a.castedNode = f;
		
		/* Create jump as target for continue statements */
		ASMLabel continueJump = new ASMLabel(LabelUtil.getLabel());
		f.continueJump = continueJump;
		
		/* Open new seperate scope for iterator, since iterator is persistent between iterations. */
		st.openScope(a);
		
		Declaration dec = null;
		if (a.iterator instanceof Declaration) dec = (Declaration) a.iterator;
		
		/* Initialize iterator */
		if (dec != null) f.instructions.addAll(AsNDeclaration.cast(dec, r, map, st).getInstructions());
		
		if (dec != null) {
			if (r.declarationLoaded(dec)) {
				/* Check if an address reference was made to the declaration, if yes, push it on the stack. */
				boolean push = false;
				for (Statement s : a.body)
					push |= Matcher.hasAddressReference(s, dec);
				
				if (push) {
					int reg = r.declarationRegLocation(dec);
					
					f.instructions.add(new ASMPushStack(new RegOp(reg)));
					
					st.push(dec);
					r.free(reg);
				}
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
		
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(a.condition, r, map, st);
		
		if (expr instanceof AsNCompare com) {
			/* Top Comparison */

			COND neg = com.neg;
			
			/* Remove two conditional mov instrutions */
			com.instructions.remove(com.instructions.size() - 1);
			com.instructions.remove(com.instructions.size() - 1);
			
			/* Evaluate Condition */
			f.instructions.addAll(com.getInstructions());
			
			/* Condition was false, no else, skip body */
			f.instructions.add(new ASMBranch(BRANCH_TYPE.B, neg, new LabelOp(forEnd)));
		}
		else {
			/* Default condition evaluation */
			f.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			f.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			
			/* Condition was false, jump to else */
			f.instructions.add(new ASMBranch(BRANCH_TYPE.B, COND.EQ, new LabelOp(forEnd)));
		}
		
		
		/* Add body, dont use addBody() because of custom scope handling */
		for (Statement s : a.body) 
			f.loadStatement(a, s, r, map, st);
		
		/* Add jump for continue statements to use as target */
		f.instructions.add(continueJump);
		
		/* Add increment */
		if (a.increment instanceof AssignWriteback awb)
			f.instructions.addAll(AsNAssignWriteback.cast(awb, r, map, st, false).getInstructions());
		else f.instructions.addAll(AsNAssignment.cast(a.increment, r, map, st).getInstructions());
		
		/* Free all declarations in scope */
		popDeclarationScope(f, a, r, st, map, true);
		
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(forStart));
		branch.optFlags.add(OPT_FLAG.LOOP_BRANCH);
		f.instructions.add(branch);
		
		
		/* Add loop end */
		f.instructions.add(forEnd);
		
		/* Remove iterator from register or stack */
		if (r.declarationLoaded(dec)) {
			int loc = r.declarationRegLocation(dec);
			r.getReg(loc).free();
		}
		else if (r.getVRegSet().declarationLoaded(dec)) {
			int loc = r.getVRegSet().declarationRegLocation(dec);
			r.getVRegSet().getReg(loc).free();
		}
		else {
			int add = st.closeScope(a, true);
			if (add != 0) {
				f.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(add)));
			}
		}
		
		f.freeDecs(r, a);
		f.registerMetric();
		return f;
	}
	
} 
