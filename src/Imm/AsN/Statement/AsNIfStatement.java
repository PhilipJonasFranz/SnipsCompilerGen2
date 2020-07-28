package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.IfStatement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;

public class AsNIfStatement extends AsNConditionalCompoundStatement {

	public static AsNIfStatement cast(IfStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNIfStatement if0 = new AsNIfStatement();
		
		AsNExpression expr = AsNExpression.cast(a.condition, r, map, st);
		
		if (expr instanceof AsNCmp) {
			if0.topComparison(a, (AsNCmp) expr, r, map, st);
			if0.freeDecs(r, a);
			return if0;
		}
		else {
			if0.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			if0.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			
			ASMLabel elseTarget = new ASMLabel(LabelGen.getLabel());
			
			ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
			
			/* Condition was false, jump to else */
			if (a.elseStatement == null) {
				if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(endTarget)));
			}
			else if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(elseTarget)));
			
			/* Add Body */
			if0.addBody(a, r, map, st);
			
			if (a.elseStatement != null) if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endTarget)));
			
			IfStatement elseS = a.elseStatement;
			if (elseS != null) if0.instructions.add(elseTarget);
			while (elseS != null) {
				if (elseS.condition != null) {
					if0.instructions.addAll(AsNExpression.cast(elseS.condition, r, map, st).getInstructions());
					
					if0.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					if (elseS.elseStatement != null)
						if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(elseTarget)));
					else 
						if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(endTarget)));
				}
				
				/* Add Body */
				if0.addBody(elseS, r, map, st);
				
				if (elseS.elseStatement != null) {
					/* Jump to end after body */
					if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endTarget)));
					if0.instructions.add(elseTarget);
				}
				else break;
				
				elseS = elseS.elseStatement;
			}
			
			if0.instructions.add(endTarget);
			
			if0.freeDecs(r, a);
			return if0;
		}
	}
	
	protected void topComparison(IfStatement a, AsNCmp com, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
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
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOp(elseTarget)));
		}
		else {
			/* Condition was false, no else, skip body */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOp(endTarget)));
		}
		
		/* Add Body */
		this.addBody(a, r, map, st);
		
		if (a.elseStatement != null) this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endTarget)));
		
		/* ElseIf / Else Exists, needs jump to next case */
		if (elseS != null) this.instructions.add(elseTarget);
		
		while (elseS != null) {
			/* Else If Statement */
			if (elseS.condition != null) {
				AsNExpression expr = AsNExpression.cast(elseS.condition, r, map, st);
				
				if (expr instanceof AsNCmp) {
					this.topComparison(elseS, (AsNCmp) expr, r, map, st);
					this.instructions.add(endTarget);
					return;
				}
				else {
					this.instructions.addAll(expr.getInstructions());
					
					this.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
					
					elseTarget = new ASMLabel(LabelGen.getLabel());
				
					/* False Jump */
					if (elseS.elseStatement != null)
						this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(elseTarget)));
					else 
						this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(endTarget)));
				}
			}
			
			/* Add Body */
			this.addBody(elseS, r, map, st);
			
			if (elseS.elseStatement != null) {
				/* Jump to end */
				this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(endTarget)));
				this.instructions.add(elseTarget);
			}
			else break;
			
			elseS = elseS.elseStatement;
		}
		
		/* End Target Destination */
		this.instructions.add(endTarget);
	}
	
} 
