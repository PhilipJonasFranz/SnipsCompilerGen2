package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCompare;
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

public class AsNIfStatement extends AsNStatement {

	public AsNIfStatement() {
		
	}

	public static AsNIfStatement cast(IfStatement a, RegSet r) throws CGEN_EXCEPTION {
		AsNIfStatement if0 = new AsNIfStatement();
		
		AsNExpression expr = AsNExpression.cast(a.condition, r);
		
		if (expr instanceof AsNCmp) {
			if0.topComparison(a, (AsNCmp) expr, r);
			return if0;
		}
		else {
			if0.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			if0.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			ASMLabel elseTarget = new ASMLabel(LabelGen.getLabel());
			/* Condition was false, jump to else */
			if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
			
			ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
			
			/* True Body */
			for (Statement s : a.body) {
				if0.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
			}
			
			if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
			
			IfStatement elseS = a.elseStatement;
			if (elseS != null) if0.instructions.add(elseTarget);
			while (elseS != null) {
				if (elseS.condition != null) {
					if0.instructions.addAll(AsNExpression.cast(elseS.condition, r).getInstructions());
					
					if0.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new ImmOperand(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
				}
				
				/* Body */
				for (Statement s : elseS.body) {
					if0.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
				}
				
				if (elseS.condition != null) {
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
	
	protected void topComparison(IfStatement a, AsNCmp com, RegSet r) throws CGEN_EXCEPTION {
		COND neg = com.neg;
		
		/* Remove Conditional results */
		com.instructions.remove(com.instructions.size() - 1);
		com.instructions.remove(com.instructions.size() - 1);
		
		this.instructions.addAll(com.getInstructions());
		
		IfStatement elseS = a.elseStatement;
		
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
		
		/* True Body */
		for (Statement s : a.body) {
			this.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
		}
		
		this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
		
		/* ElseIf / Else Exists, needs jump to next case */
		if (elseS != null) this.instructions.add(elseTarget);
		
		while (elseS != null) {
			if (elseS.condition != null) {
				AsNExpression expr = AsNExpression.cast(elseS.condition, r);
				
				if (expr instanceof AsNCmp) {
					this.topComparison(elseS, (AsNCmp) expr, r);
					return;
				}
				else {
					this.instructions.addAll(expr.getInstructions());
					
					this.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new ImmOperand(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(elseTarget)));
				}
			}
			
			/* Body */
			for (Statement s : elseS.body) {
				this.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
			}
			
			if (elseS.condition != null) {
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
