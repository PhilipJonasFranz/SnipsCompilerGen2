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
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Arith.AsNCmp;

public class AsNIfStatement extends AsNConditionalCompoundStatement {

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
			
			/* Add Body */
			if0.addBody(a, r, st);
			
			if (a.elseStatement != null) if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
			
			IfStatement elseS = a.elseStatement;
			if (elseS != null) if0.instructions.add(elseTarget);
			while (elseS != null) {
				if (elseS.condition != null) {
					if0.instructions.addAll(AsNExpression.cast(elseS.condition, r, st).getInstructions());
					
					if0.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
				}
				
				/* Add Body */
				if0.addBody(elseS, r, st);
				
				if (elseS.elseStatement != null) {
					/* Jump to end after body */
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
	
	protected void topComparison(IfStatement a, AsNCmp com, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		COND neg = com.neg;
		
		/* Remove Conditional results */
		com.instructions.remove(com.instructions.size() - 1);
		com.instructions.remove(com.instructions.size() - 1);
		
		this.instructions.addAll(com.getInstructions());
		
		IfStatement elseS = a.elseStatement;
		
		/* The target of the if/elseif/else chain */
		ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
		
		ASMLabel elseTarget = new ASMLabel(LabelGen.getLabel());
		if (elseS != null) {
			/* Condition was false, jump to else */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(elseTarget)));
		}
		else {
			/* Condition was false, no else, skip body */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(endTarget)));
		}
		
		/* Add Body */
		this.addBody(a, r, st);
		
		if (a.elseStatement != null) this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
		
		/* ElseIf / Else Exists, needs jump to next case */
		if (elseS != null) this.instructions.add(elseTarget);
		
		while (elseS != null) {
			/* Else If Statement */
			if (elseS.condition != null) {
				AsNExpression expr = AsNExpression.cast(elseS.condition, r, st);
				
				if (expr instanceof AsNCmp) {
					this.topComparison(elseS, (AsNCmp) expr, r, st);
					this.instructions.add(endTarget);
					return;
				}
				else {
					this.instructions.addAll(expr.getInstructions());
					
					this.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
				}
			}
			
			/* Add Body */
			this.addBody(elseS, r, st);
			
			if (elseS.elseStatement != null) {
				/* Jump to end */
				this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
				this.instructions.add(elseTarget);
			}
			else break;
			
			elseS = elseS.elseStatement;
		}
		
		/* End Target Destination */
		this.instructions.add(endTarget);
	}
	
}
