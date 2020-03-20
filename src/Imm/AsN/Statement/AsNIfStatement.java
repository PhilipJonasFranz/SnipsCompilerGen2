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
import Imm.AsN.Expression.Arith.AsNCompare;

public class AsNIfStatement extends AsNStatement {

	public AsNIfStatement() {
		
	}

	public static AsNIfStatement cast(IfStatement a, RegSet r) throws CGEN_EXCEPTION {
		AsNIfStatement if0 = new AsNIfStatement();
		
		AsNExpression expr = AsNExpression.cast(a.condition, r);
		
		if (expr instanceof AsNCompare) {
			if0.topComparison(a, (AsNCompare) expr, r);
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
	
	protected void topComparison(IfStatement a, AsNCompare com, RegSet r) throws CGEN_EXCEPTION {
		COND neg = com.neg;
		
		/* Remove Conditional results */
		com.instructions.remove(com.instructions.size() - 1);
		com.instructions.remove(com.instructions.size() - 1);
		
		this.instructions.addAll(com.getInstructions());
		
		IfStatement elseS = a.elseStatement;
		
		ASMLabel elseTarget = new ASMLabel(LabelGen.getLabel());
		/* Condition was false, jump to else */
		if (elseS != null) this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(elseTarget)));
		
		ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
		
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
				
				if (expr instanceof AsNCompare) {
					this.topComparison(elseS, (AsNCompare) expr, r);
					return;
				
					/*AsNCompare com0 = (AsNCompare) expr;
					COND neg0 = com0.neg;
					
					com0.instructions.remove(com0.instructions.size() - 1);
					com0.instructions.remove(com0.instructions.size() - 1);
				
					this.instructions.addAll(com0.getInstructions());
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
					
					this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg0), new LabelOperand(elseTarget)));*/
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
