package Imm.AsN.Expression.Boolean;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AsN.Expression.AsNExpression;

public class AsNTernary extends AsNExpression {

			/* --- METHODS --- */
	public static AsNTernary cast(Ternary t, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNTernary tern = new AsNTernary();
		
		tern.clearReg(r, st, 0, 1, 2);
		
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(t.condition, r, map, st);
		
		/* The Target that is branched to if the condition is false */
		ASMLabel loadFalse = new ASMLabel(LabelGen.getLabel());
		
		/* The End Label Target */
		ASMLabel end = new ASMLabel(LabelGen.getLabel());
		
		if (expr instanceof AsNCmp) {
			/* Top Comparison */
			AsNCmp com = (AsNCmp) expr;
			
			COND neg = com.neg;
			
			/* Remove two conditional mov instrutions */
			com.instructions.remove(com.instructions.size() - 1);
			com.instructions.remove(com.instructions.size() - 1);
			
			/* Evaluate Condition */
			tern.instructions.addAll(com.getInstructions());
			
			/* Condition was false, no else, skip first result */
			tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(loadFalse)));
		}
		else {
			/* Default condition evaluation */
			tern.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			tern.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			/* Condition was false, jump to else */
			tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(loadFalse)));
		}
		
		/* Load true result */
		tern.instructions.addAll(AsNExpression.cast(t.leftOperand, r, map, st).getInstructions());
		
		/* Branch to end */
		tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(end)));
		
		/* False Target */
		tern.instructions.add(loadFalse);
		
		/* Load false result */
		tern.instructions.addAll(AsNExpression.cast(t.rightOperand, r, map, st).getInstructions());
		
		/* Add end */
		tern.instructions.add(end);
		
		r.free(0, 1, 2);
		
		return tern;
	}
	
}
