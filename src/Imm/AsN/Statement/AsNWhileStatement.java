package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;

public class AsNWhileStatement extends AsNConditionalCompoundStatement {

	public static AsNWhileStatement cast(WhileStatement a, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNWhileStatement w = new AsNWhileStatement();
		
		AsNExpression expr = AsNExpression.cast(a.condition, r, st);
		
		if (expr instanceof AsNCmp) {
			w.topComparison(a, (AsNCmp) expr, r, st);
		}
		else {
			/* Loop Entrypoint */
			ASMLabel whileStart = new ASMLabel(LabelGen.getLabel());
			w.instructions.add(whileStart);
			
			/* Evaluate Condition */
			w.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			w.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			ASMLabel whileEnd = new ASMLabel(LabelGen.getLabel());
			
			/* Condition was false, jump to else */
			w.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(whileEnd)));
			
			/* Add Body */
			w.addBody(a, r, st);
			
			/* Branch to loop start */
			w.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(whileStart)));
			
			/* While End Label */
			w.instructions.add(whileEnd);
		}
		
		return w;
	}
	
	protected void topComparison(WhileStatement a, AsNCmp com, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		
		ASMLabel whileStart = new ASMLabel(LabelGen.getLabel());
		this.instructions.add(whileStart);
		
		COND neg = com.neg;
		
		/* Remove two conditional mov instrutions */
		com.instructions.remove(com.instructions.size() - 1);
		com.instructions.remove(com.instructions.size() - 1);
		
		/* Evaluate Condition */
		this.instructions.addAll(com.getInstructions());
		
		ASMLabel whileEnd = new ASMLabel(LabelGen.getLabel());
		
		/* Condition was false, no else, skip body */
		this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(whileEnd)));
		
		/* Add Body */
		this.addBody(a, r, st);
		
		/* Branch to loop Start */
		this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(whileStart)));
		
		this.instructions.add(whileEnd);
	}
	
}
