package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.Expression.AsNExpression;

public class AsNIfStatement extends AsNStatement {

	public AsNIfStatement() {
		
	}

	public static AsNIfStatement cast(IfStatement a, RegSet r) throws CGEN_EXCEPTION {
		AsNIfStatement if0 = new AsNIfStatement();
		
		if0.instructions.addAll(AsNExpression.cast(a.condition, r).getInstructions());
		
		//if0.instructions.add(new ASMCompare(new RegOperand(REGISTER.R0), new ImmOperand(0)));
		
		ASMLabel falseTarget = new ASMLabel(LabelGen.getLabel());
		/* False Jump */
		if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(falseTarget)));
		
		ASMLabel endTarget = new ASMLabel(LabelGen.getLabel());
		
		/* True Body */
		for (Statement s : a.body) {
			if0.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
		}
		
		if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
		
		if0.instructions.add(falseTarget);
		
		IfStatement elseS = a.elseStatement;
		while (elseS != null) {
			if (elseS.condition != null) {
				if0.instructions.addAll(AsNExpression.cast(elseS.condition, r).getInstructions());
				
				falseTarget = new ASMLabel(LabelGen.getLabel());
			
				/* False Jump */
				if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(falseTarget)));
			}
			
			/* Body */
			for (Statement s : elseS.body) {
				if0.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
			}
			
			if (elseS.condition != null) {
				/* Jump to end */
				if0.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(endTarget)));
				
				if0.instructions.add(falseTarget);
			}
			else break;
			
			elseS = elseS.elseStatement;
		}
		
		/* Only add if no else statement exists */
		if0.instructions.add(endTarget);
		
		return if0;
	}
	
}
