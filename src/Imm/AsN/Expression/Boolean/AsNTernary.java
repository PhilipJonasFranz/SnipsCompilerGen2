package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
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
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AsN.Expression.AsNExpression;

public class AsNTernary extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTernary cast(Ternary t, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNTernary tern = new AsNTernary();
		
		r.free(0, 1, 2);
		
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(t.condition, r, map, st);
		
		/* The Target that is branched to if the condition is false */
		ASMLabel loadFalse = new ASMLabel(LabelUtil.getLabel());
		
		/* The End Label Target */
		ASMLabel end = new ASMLabel(LabelUtil.getLabel());
		
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
			tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOp(loadFalse)));
		}
		else {
			/* Default condition evaluation */
			tern.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			tern.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			
			/* Condition was false, jump to else */
			tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(loadFalse)));
		}
		
		/* Load true result */
		tern.instructions.addAll(AsNExpression.cast(t.leftOperand, r, map, st).getInstructions());
		
		/* Branch to end */
		tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(end)));
		
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
