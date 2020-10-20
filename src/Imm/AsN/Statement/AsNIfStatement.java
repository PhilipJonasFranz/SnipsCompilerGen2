package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Statement.IfStatement;
import Imm.AsN.Expression.AsNExpression;

public class AsNIfStatement extends AsNConditionalCompoundStatement {

	public static AsNIfStatement cast(IfStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNIfStatement if0 = new AsNIfStatement();
		
		/* Used to iterate over if-chain */
		IfStatement currentIf = a;
		
		ASMLabel endTarget = new ASMLabel(LabelUtil.getLabel());
		
		while (currentIf != null) {
			ASMLabel elseTarget = new ASMLabel(LabelUtil.getLabel());
		
			/* Else If Statement */
			if (currentIf.condition != null) {
				COND cond = if0.injectConditionEvaluation(AsNExpression.cast(currentIf.condition, r, map, st));
				
				if (currentIf.elseStatement != null) 
					/* Condition was false, jump to else */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(cond), new LabelOp(elseTarget)));
				else 
					/* Condition was false, no else, skip body */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(cond), new LabelOp(endTarget)));
			}
			
			/* Add Body */
			if0.addBody(currentIf, r, map, st);
			
			if (currentIf.elseStatement != null) {
				/* Jump to end */
				if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endTarget)));
				if0.instructions.add(elseTarget);
			}
			else break;
			
			currentIf = currentIf.elseStatement;
		}
		
		if0.instructions.add(endTarget);
		
		if0.freeDecs(r, a);
		return if0;
	}
	
} 
