package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;

public class AsNForStatement extends AsNConditionalCompoundStatement {

	public static AsNForStatement cast(ForStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNForStatement f = new AsNForStatement();
		a.castedNode = f;
		
		/* Create jump as target for continue statements */
		ASMLabel continueJump = new ASMLabel(LabelGen.getLabel());
		f.continueJump = continueJump;
		
		/* Open new scope for iterator */
		st.openScope();
		
		/* Initialize iterator */
		f.instructions.addAll(AsNDeclaration.cast(a.iterator, r, map, st).getInstructions());
		
		/* Open scope for condition, body and increment statement */
		st.openScope();
		
		/* Marks the start of the loop */
		ASMLabel forStart = new ASMLabel(LabelGen.getLabel());
		f.instructions.add(forStart);
		
		/* End of the loop */
		ASMLabel forEnd = new ASMLabel(LabelGen.getLabel());
		
		/* Set jump target for break statements */
		f.breakJump = forEnd;
		
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(a.condition, r, map, st);
		
		if (expr instanceof AsNCmp) {
			/* Top Comparison */
			AsNCmp com = (AsNCmp) expr;
			
			COND neg = com.neg;
			
			/* Remove two conditional mov instrutions */
			com.instructions.remove(com.instructions.size() - 1);
			com.instructions.remove(com.instructions.size() - 1);
			
			/* Evaluate Condition */
			f.instructions.addAll(com.getInstructions());
			
			/* Condition was false, no else, skip body */
			f.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(forEnd)));
		}
		else {
			/* Default condition evaluation */
			f.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			f.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			/* Condition was false, jump to else */
			f.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(forEnd)));
		}
		
		
		/* Add body */
		for (Statement s : a.body) 
			f.instructions.addAll(AsNStatement.cast(s, r, map, st).getInstructions());
		
		/* Add jump for continue statements to use as target */
		f.instructions.add(continueJump);
		
		/* Add increment */
		f.instructions.addAll(AsNAssignment.cast(a.increment, r, map, st).getInstructions());
		
		/* Free all declarations in scope */
		f.popDeclarationScope(a, r, st);
		
		
		/* Branch to loop start */
		f.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(forStart)));
		
		/* Add loop end */
		f.instructions.add(forEnd);
		
		/* Remove iterator from register or stack */
		if (r.declarationLoaded(a.iterator)) {
			int loc = r.declarationRegLocation(a.iterator);
			r.getReg(loc).free();
		}
		else {
			int add = st.closeScope();
			if (add != 0) {
				f.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(add)));
			}
		}
		
		return f;
	}
	
}
