package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.ASMCmp;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Arith.AsNCmp;

public class AsNIfStatement extends AsNConditionalCapsuledStatement {

	public AsNIfStatement() {
		
	}

	public static AsNIfStatement cast(IfStatement a, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNIfStatement if0 = new AsNIfStatement();
		
		AsNExpression expr = AsNExpression.cast(a.condition, r, st);
		
		if (expr instanceof AsNCmp) {
			if0.topComparison(a, (AsNCmp) expr, r, st);
			return if0;
		}
		else {
			if0.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			if0.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			ASMLabel elseTarget = new ASMLabel(LabelGen.getLabel());
			/* Condition was false, jump to else */
			if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
			
			ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
			
			/* True Body */
			for (Statement s : a.body) {
				if0.instructions.addAll(AsNStatement.cast(s, r, st).getInstructions());
			}
			
			if0.popDeclarationScope(a, r, st);
			
			if (a.elseStatement != null) if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
			
			IfStatement elseS = a.elseStatement;
			if (elseS != null) if0.instructions.add(elseTarget);
			while (elseS != null) {
				if (elseS.condition != null) {
					if0.instructions.addAll(AsNExpression.cast(elseS.condition, r, st).getInstructions());
					
					if0.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
				}
				
				/* Body */
				for (Statement s : elseS.body) {
					if0.instructions.addAll(AsNStatement.cast(s, r, st).getInstructions());
				}
				
				/* Free all declarations in scope */
				if0.popDeclarationScope(a, r, st);
				
				if (elseS.elseStatement != null) {
					/* Jump to end */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
					if0.instructions.add(elseTarget);
				}
				else break;
				
				elseS = elseS.elseStatement;
			}
			
			if0.instructions.add(endTarget);
			
			return if0;
		}
	}
	
}
